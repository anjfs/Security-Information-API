package sources;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class CrawlerHelper {

    private static StringBuilder builder;

    Crawler crawler;

    public CrawlerHelper(Crawler crawler) {
        this.crawler = crawler;
    }

    public void queryCVE(){
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(crawler.getNewFile()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        builder = new StringBuilder();
        String columnNamesList = "CVE_ID;Description;Type;Type-of-source;Weakness;Date;Source;Reference;Score;Severity";
        builder.append(columnNamesList + "\n");

        WebClient client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setThrowExceptionOnFailingStatusCode(false);

        int number = 1;
        final DecimalFormat decimalFormat = new DecimalFormat("0000");

        while (number <= crawler.getNumQueries()) {
            try {
                String searchUrl = "https://nvd.nist.gov/vuln/detail/CVE-" + URLEncoder.encode(String.valueOf(crawler.getYear()), "UTF-8") + "-" + URLEncoder.encode(decimalFormat.format(number), "UTF-8");
                HtmlPage page = client.getPage(searchUrl);

                // check if page returns CVE ID not found
                ArrayList<DomText> error = (ArrayList<DomText>) page.getByXPath("/html/body/div[2]/div[2]/div[2]/h2");
                if(!error.isEmpty()){
                    number++;
                    continue;
                }

                String id = "CVE-" + crawler.getYear() + "-" + decimalFormat.format(number);
                //System.out.println(id);

                ArrayList<DomText> list = (ArrayList<DomText>) page.getByXPath("//*[@id=\"vulnDetailTableView\"]/tbody/tr/td/div/div[1]/p/text()");
                final DomText desc = list.get(0);
                //System.out.println("   Description: " + desc.asText());

                // skip CVE if it is rejected
                if(desc.asText().contains("** REJECT **")){
                    number++;
                    continue;
                }

                //System.out.println("   Type-of-source: Vulnerability Database");
                //System.out.println("   Type: Vulnerability");

                ArrayList<HtmlSpan> date = (ArrayList<HtmlSpan>) page.getByXPath("//*[@id=\"vulnDetailTableView\"]/tbody/tr/td/div/div[2]/div/span[1]");
                final HtmlSpan dateText = date.get(0);
                //System.out.println("   Date: " + dateText.asText());

                ArrayList<HtmlSpan> source = (ArrayList<HtmlSpan>) page.getByXPath("//*[@id=\"vulnDetailTableView\"]/tbody/tr/td/div/div[2]/div/span[3]");
                final HtmlSpan sourceText = source.get(0);
                //System.out.println("   Source: " + sourceText.asText());

                String weak = "Not defined";
                ArrayList<DomText> weaknessList = (ArrayList<DomText>) page.getByXPath("//*[@id=\"vulnTechnicalDetailsDiv\"]/table/tbody/tr/td[2]/text()");
                if(!weaknessList.isEmpty()) {
                    final DomText weakness = weaknessList.get(0);
                    weak = weakness.asText();
                    //System.out.println("   Weakness: " + weakness.asText());
                }

                HtmlAnchor severityRating = null;
                ArrayList<HtmlAnchor> rating = (ArrayList<HtmlAnchor>) page.getByXPath("//*[@id=\"Cvss3NistCalculatorAnchor\"]");
                if (!rating.isEmpty()) {
                    severityRating = rating.get(0);
                    //System.out.println("   Severity-rating: " + severityRating.asText());
                } else {
                    ArrayList<HtmlAnchor> rating2 = (ArrayList<HtmlAnchor>) page.getByXPath("//*[@id=\"Cvss3CnaCalculatorAnchor\"]");
                    if (!rating2.isEmpty()) {
                        severityRating = rating2.get(0);
                        //System.out.println("   Severity-rating: " + severityRating.asText());
                    }
                }

                String score = "";
                String severityText = "";
                if(severityRating != null){
                    String severity = severityRating.asText();
                    String[] split = severity.split(" ");
                    score = split[0];
                    severityText = split[1];
                } else {
                    score = "No score";
                    severityText = "No severity rating";
                }

                //System.out.println("   Reference(s):");
                ArrayList<HtmlElement> listTable = (ArrayList<HtmlElement>) page.getByXPath("//*[@id=\"vulnHyperlinksPanel\"]/table");
                final HtmlTable tableLinks = (HtmlTable) listTable.get(0);
                for (final HtmlTableRow row : tableLinks.getRows()) {
                    //System.out.println("Found match:");
                    for (final HtmlTableCell cell : row.getCells()) {
                        if (cell.asText().startsWith("http")) {
                            //System.out.println("      " + cell.asText());
                        }
                    }
                }

                //System.out.println("   CVE-link: " + "https://cve.mitre.org/cgi-bin/cvename.cgi?name=" + id);
                //System.out.println();

                builder.append(id + ";");
                builder.append(desc.asText() + ";");
                builder.append("Vulnerability" + ";");
                builder.append("Vulnerability Database" + ";");
                builder.append(weak + ";");
                builder.append(dateText.asText() + ";");
                builder.append(sourceText.asText() + ";");
                builder.append("https://cve.mitre.org/cgi-bin/cvename.cgi?name=" + id + ";");
                builder.append(score + ";");
                builder.append(severityText);
                builder.append('\n');

                number++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        pw.write(builder.toString());
        pw.close();
    }

    public void queryCWE(){
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(crawler.getNewFile()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder = new StringBuilder();
        String columnNamesList = "CWE_ID;Description;Type;Type-of-source;Weakness;Date;Source";
        builder.append(columnNamesList + "\n");

        WebClient client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setThrowExceptionOnFailingStatusCode(false);

        int number = 1;
        while (number <= crawler.getNumQueries()) {
            try {
                String searchUrl = "https://cwe.mitre.org/data/definitions/" + URLEncoder.encode(String.valueOf(number), "UTF-8") +".html";
                HtmlPage page = client.getPage(searchUrl);

                ArrayList<HtmlHeading2> titleList = (ArrayList<HtmlHeading2>) page.getByXPath("//*[@id=\"Contentpane\"]/div[2]/h2");
                if(titleList.isEmpty()){
                    number++;
                    continue;
                }
                final HtmlHeading2 weakness = titleList.get(0);
                if(weakness.asText().contains("CATEGORY") || weakness.asText().contains("DEPRECATED") || weakness.asText().contains("deprecated") || weakness.asText().contains("Deprecated") || weakness.asText().contains("Weaknesses")){
                    number++;
                    continue;
                }

                String id = "CWE-" + number;
                System.out.println(id);

                String weaknessText = "";
                if(weakness.asText() != null){
                    String weak = weakness.asText();
                    String[] split = weak.split(":", 2);
                    weaknessText = split[1];
                    weaknessText = weaknessText.substring(1);
                }

                System.out.println("   Weakness: " + weaknessText);

                String xpathDesc = "//*[@id=\"oc_" + number + "_Description\"]/div/div/text()";
                ArrayList<DomText> list = (ArrayList<DomText>) page.getByXPath(xpathDesc);
                if(list.isEmpty()){
                    number++;
                    continue;
                }
                final DomText desc = list.get(0);
                System.out.println("   Description: " + desc.asText());

//                String notes = "";
//                String xpathNotes = "//*[@id=\"oc_" + number + "_Notes\"]/div/div/div";
//                ArrayList<HtmlDivision> notesList = (ArrayList<HtmlDivision>) page.getByXPath(xpathNotes);
//                if(!notesList.isEmpty()) {
//                    final HtmlDivision note = notesList.get(0);
//                    notes = note.asText();
//                    System.out.println("   Notes: " + note.asText());
//                }

                System.out.println("   Type-of-source: Weakness Database");
                System.out.println("   Type: Weakness");

                String xpathDate = "//*[@id=\"oc_" + number + "_Submissions\"]/tr[2]/td[1]";
                ArrayList<HtmlTableDataCell> date = (ArrayList<HtmlTableDataCell>) page.getByXPath(xpathDate);
                final HtmlTableDataCell dateText = date.get(0);
                System.out.println("   Date: " + dateText.asText());

                System.out.println("   CWE-link: " + "https://cve.mitre.org/cgi-bin/cvename.cgi?name=" + id);
                System.out.println();

                builder.append(id + ";");
                builder.append(desc.asText().replace("\n", " ") +";");
                builder.append("Vulnerability" + ";");
                builder.append("Vulnerability Database" + ";");
                builder.append(weaknessText + ";");
                builder.append(dateText.asText() + ";");
                builder.append("https://cwe.mitre.org/data/definitions/" + id);
                builder.append('\n');

                number++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        pw.write(builder.toString());
        pw.close();
    }

    public void queryCAPEC(){
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(crawler.getNewFile()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder = new StringBuilder();
        String columnNamesList = "CAPEC_ID;Description;Type;Type-of-source;Weakness;Date;Source";
        builder.append(columnNamesList + "\n");

        WebClient client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setThrowExceptionOnFailingStatusCode(false);

        int number = 1;
        while (number <= crawler.getNumQueries()) {
            try {
                String searchUrl = "https://capec.mitre.org/data/definitions/" + URLEncoder.encode(String.valueOf(number), "UTF-8") +".html";
                HtmlPage page = client.getPage(searchUrl);

                ArrayList<HtmlHeading2> titleList = (ArrayList<HtmlHeading2>) page.getByXPath("//*[@id=\"Contentpane\"]/div[1]/table/tbody/tr/td/h2");
                if(titleList.isEmpty()){
                    number++;
                    continue;
                }
                final HtmlHeading2 weakness = titleList.get(0);
                if(weakness.asText().contains("CATEGORY") || weakness.asText().contains("DEPRECATED")){
                    number++;
                    continue;
                }

                String id = "CAPEC-" + number;
                System.out.println(id);

                String weaknessText = "";
                if(weakness.asText() != null){
                    String weak = weakness.asText();
                    String[] split = weak.split(":", 2);
                    weaknessText = split[1];
                    weaknessText = weaknessText.substring(1);
                }

                System.out.println("   Weakness: " + weaknessText);

                String xpathDesc = "//*[@id=\"oc_" + number + "_Description\"]/div";
                ArrayList<HtmlDivision> list = (ArrayList<HtmlDivision>) page.getByXPath(xpathDesc);
                if(list.isEmpty()){
                    number++;
                    continue;
                }
                final HtmlDivision desc = list.get(0);
                System.out.println("   Description: " + desc.asText());

                System.out.println("   Type-of-source: Attack Database");
                System.out.println("   Type: Attack");

                String xpathDate = "//*[@id=\"footbar\"]/text()";
                ArrayList<DomText> date = (ArrayList<DomText>) page.getByXPath(xpathDate);
                final DomText dateText = date.get(0);
                System.out.println("   Date: " + dateText.asText());

                System.out.println("   CAPEC-link: " + "https://cve.mitre.org/cgi-bin/cvename.cgi?name=" + id);
                System.out.println();

                builder.append(id + ";");
                builder.append(desc.asText().replace("\n", " "));
                builder.append("Vulnerability" + ";");
                builder.append("Vulnerability Database" + ";");
                builder.append(weaknessText + ";");
                builder.append(dateText.asText() + ";");
                builder.append("https://cwe.mitre.org/data/definitions/" + id);
                builder.append('\n');

                number++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        pw.write(builder.toString());
        pw.close();
    }
}
