package sources;

import machinelearning.utils.Cleanup;
import machinelearning.utils.MergeFiles;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import similarity.TFIDFSimilarity;
import similarity.Word2VecSimilarity;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class StackExchangeAPI_Old {

    private static StringBuilder builder;
    private static StringBuilder builder1;
    private static StringBuilder builder2;
    public static final String STACKOVERFLOW = "stackoverflow";
    public static final String ASKUBUNTU = "askubuntu";
    public static final String SERVERFAULT = "severfault";
    public static final String SOFTWAREENGINEERING = "softwareengineering";

    public static void main(String[] args) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InterruptedException {

        MergeFiles m = new MergeFiles();

        String tags = "security";
        String site = "stackoverflow";
        String source = "CVE";
        int numPages = 10;
        String dataset = "/Users/anja/Desktop/master/api/files/sources/";
        String terms = "/Users/anja/Desktop/master/api/files/features/";
        String word2vec = "/Users/anja/Desktop/master/api/files/features/";
        int numFeatures = 100;

        String path = "./files/experiments/tfidf_2/";

//        getNSRs("./files/experiments/NSR.csv", site, 1000, false);
//        m.addSecurityLabel("./files/experiments/NSR.csv", "./files/experiments/NSR_sec.csv", true);
//
        //allMethods("./files/experiments/tfidf/stackoverflow_CVE/stackoverflow_SR.csv","./files/experiments/word2vec/stackoverflow_CVE/stackoverflow_SR.csv", "./files/experiments/tfidfword2vec/stackoverflow_CVE/stackoverflow_SR.csv", dataset + "cve.csv", terms + "CVEFeaturesTFIDF.txt", word2vec + "cve_word2vec_model.txt", site,  tags,0.7, 1000, numFeatures, true, true);
        //allMethods("./files/experiments/tfidf/stackoverflow_CWE/stackoverflow_SR.csv","./files/experiments/word2vec/stackoverflow_CWE/stackoverflow_SR.csv", "./files/experiments/tfidfword2vec/stackoverflow_CWE/stackoverflow_SR.csv", dataset + "cve.csv", terms + "CWEFeaturesTFIDF.txt", word2vec + "cwe_word2vec_model.txt", site, tags, 0.7, 1000, numFeatures, true, true);
        //allMethods("./files/experiments/tfidf/stackoverflow_CAPEC/stackoverflow_SR.csv","./files/experiments/word2vec/stackoverflow_CAPEC/stackoverflow_SR.csv", "./files/experiments/tfidfword2vec/stackoverflow_CAPEC/stackoverflow_SR.csv", dataset + "cve.csv", terms + "CAPECFeaturesTFIDF.txt", word2vec + "capec_word2vec_model.txt", site, tags, 0.7, 1000, numFeatures, true, true);

    }

    public static void getSRs(String newFile, String site, int numSRs, boolean getAnswers) throws UnsupportedEncodingException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(newFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder = new StringBuilder();
        String columnNamesList = "Title;Description;Id;Date";
        builder.append(columnNamesList + "\n");

        Boolean hasMore = true;
        int page = 1;
        int SRs = 0;
        while (SRs < numSRs) {
            HttpClient client = HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD).build())
                    .build();
            HttpGet request = new HttpGet("https://api.stackexchange.com/2.2/questions?page=" + page + "&pagesize=100&order=desc&sort=activity&site=" + site + "&filter=!--1nZwT3Ejsm&key=IT8vJtd)vD02vi1lzs5mHg((");

            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();

                // Read the contents of an entity and return it as a String.
                String content = EntityUtils.toString(entity);
                System.out.println(content);

                JSONObject result = new JSONObject(content);

                try {
                    hasMore = result.getBoolean("has_more");
                } catch (Exception e) {
                    hasMore = false;
                }

                JSONArray tokenList = new JSONArray();
                try {
                    tokenList = result.getJSONArray("items");
                } catch (Exception e) {
                    break;
                }
                for (int i = 0; i < tokenList.length(); i++) {
                    JSONObject oj = tokenList.getJSONObject(i);

                    boolean security = false;

                    JSONArray tags = oj.getJSONArray("tags");
                    for (int t = 0; t < tags.length(); t++) {
                        if (tags.get(t).toString().contains("security")) {
                            security = true;
                            break;
                        }
                    }

                    if (security && SRs < numSRs) {
                        String title = oj.getString("title");
                        title = title.replace(";", "");

                        int id = oj.getInt("question_id");
                        int date = oj.getInt("creation_date");
                        Date time = new Date((long) date * 1000);

                        String newTime = new SimpleDateFormat("dd-MM-yyyy").format(time);

                        String body = oj.getString("body");
                        String cleanText = html2text(body);

                        Boolean is_answered = oj.getBoolean("is_answered");
                        if (getAnswers && is_answered) {
                            int answerNumber = oj.getInt("answer_count");
                            if (answerNumber != 0) {
                                JSONArray answers = oj.getJSONArray("answers");

                                for (int j = 0; j < answerNumber; j++) {
                                    JSONObject answerObj = answers.getJSONObject(j);
                                    String answer = answerObj.getString("body");
                                    cleanText = cleanText + " " + html2text(answer);
                                }
                            }
                        }

                        cleanText = cleanText.replace("\n", "").replace("\r", "").replace(";", "");

                        builder.append(title + ";");
                        builder.append(cleanText + ";");
                        builder.append(id + ";");
                        builder.append(newTime);
                        builder.append('\n');
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (hasMore) {
                page++;
            } else {
                break;
            }
        }
        pw.write(builder.toString());
        pw.close();
        System.out.println("done!");
    }

    public static void getNSRs(String newFile, String site, int numNSRs, boolean getAnswers) throws UnsupportedEncodingException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(newFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder = new StringBuilder();
        String columnNamesList = "Title;Description;Id;Date";
        builder.append(columnNamesList + "\n");

        boolean hasMore = true;
        int page = 1;
        int NSRs = 0;
        while (NSRs < numNSRs) {
            HttpClient client = HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD).build())
                    .build();
            HttpGet request = new HttpGet("https://api.stackexchange.com/2.2/questions?page=" + page + "&pagesize=100&order=desc&sort=activity&&site=" + site + "&filter=!--1nZwT3Ejsm&key=IT8vJtd)vD02vi1lzs5mHg((");

            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();

                // Read the contents of an entity and return it as a String.
                String content = EntityUtils.toString(entity);

                JSONObject result = new JSONObject(content);

                hasMore = result.getBoolean("has_more");

                JSONArray tokenList = new JSONArray();
                try {
                    tokenList = result.getJSONArray("items");
                } catch (Exception e) {
                    break;
                }
                for (int i = 0; i < tokenList.length(); i++) {
                    JSONObject oj = tokenList.getJSONObject(i);
                    String title = oj.getString("title");
                    title = title.replace(";", "");

                    boolean security = false;

                    JSONArray tags = oj.getJSONArray("tags");
                    for (int j = 0; j < tags.length(); j++) {
                        if (tags.get(j).toString().contains("security")) {
                            security = true;
                            break;
                        }
                    }

                    if (!security && NSRs < numNSRs) {
                        int id = oj.getInt("question_id");
                        int date = oj.getInt("creation_date");
                        Date time = new Date((long) date * 1000);

                        String newTime = new SimpleDateFormat("dd-MM-yyyy").format(time);

                        String body = oj.getString("body");
                        String cleanText = html2text(body);

                        Boolean is_answered = oj.getBoolean("is_answered");
                        if (getAnswers && is_answered) {
                            int answerNumber = oj.getInt("answer_count");
                            if (answerNumber != 0) {
                                JSONArray answers = oj.getJSONArray("answers");

                                for (int j = 0; j < answerNumber; j++) {
                                    JSONObject answerObj = answers.getJSONObject(j);
                                    String answer = answerObj.getString("body");
                                    cleanText = cleanText + " " + html2text(answer);
                                }
                            }
                        }

                        cleanText = cleanText.replace("\n", "").replace("\r", "").replace(";", "");

                        builder.append(title + ";");
                        builder.append(cleanText + ";");
                        builder.append(id + ";");
                        builder.append(newTime);
                        builder.append('\n');
                        NSRs++;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (hasMore) {
                page++;
            } else {
                break;
            }
        }
        pw.write(builder.toString());
        pw.close();
        System.out.println("done!");
    }

    // using the average of the highest tfidf and word2vec score under a set threshold
    public static void getNSRsWithThreshold(String newFile, String benchmarkDataset, String terms, String word2vec, String site, double threshold, int numNSRs, int numFeatures, boolean getAnswers, boolean getAnswersWithThreshold, boolean appendScoreToCSV) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(newFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder = new StringBuilder();
        String columnNamesList = "";
        if (appendScoreToCSV) {
            columnNamesList = "Title;Description;Id;Date;cossim";
        } else {
            columnNamesList = "Title;Description;Id;Date";
        }
        builder.append(columnNamesList + "\n");

        TFIDFSimilarity d = new TFIDFSimilarity();

        List<String> features = d.getTermsFromFile(terms);

        List<String[]> docsArray = d.getDocsArrayFromCsv(benchmarkDataset);
        List<double[]> tfidfDocsVector = d.tfIdfCalculator(docsArray, docsArray, features);

        Word2VecSimilarity w = new Word2VecSimilarity();

        Word2Vec model = w.getWord2Vec(word2vec);

        List<Collection<String>> benchmarkSentences = new ArrayList<>();
        w.getSentences(benchmarkDataset, benchmarkSentences);
        List<INDArray> input_vectors = new ArrayList<>();

        for (int m = 0; m < benchmarkSentences.size(); m++) {
            input_vectors.add(w.getVector(benchmarkSentences.get(m), model, numFeatures));
        }

        Boolean hasMore = true;
        int page = 1;
        int NSRs = 0;
        while (NSRs < numNSRs) {
            HttpClient client = HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD).build())
                    .build();
            HttpGet request = new HttpGet("https://api.stackexchange.com/2.2/questions?page=" + page + "&pagesize=100&order=desc&sort=activity&&site=" + site + "&filter=!--1nZwT3Ejsm&key=IT8vJtd)vD02vi1lzs5mHg((");

            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();

                // Read the contents of an entity and return it as a String.
                String content = EntityUtils.toString(entity);

                JSONObject result = new JSONObject(content);

                try {
                    hasMore = result.getBoolean("has_more");
                } catch (Exception e) {
                    hasMore = false;
                }

                boolean security = false;

                JSONArray tokenList = new JSONArray();
                try {
                    tokenList = result.getJSONArray("items");
                } catch (Exception e) {
                    break;
                }

                for (int i = 0; i < tokenList.length(); i++) {
                    JSONObject oj = tokenList.getJSONObject(i);
                    String title = oj.getString("title");
                    title = title.replace(";", "");

                    JSONArray tags = oj.getJSONArray("tags");
                    for (int j = 0; j < tags.length(); j++) {
                        if (tags.get(j).toString().contains("security")) {
                            security = true;
                        }
                    }

                    if (!security && NSRs < numNSRs) {
                        int id = oj.getInt("question_id");
                        int date = oj.getInt("creation_date");
                        Date time = new Date((long) date * 1000);

                        String newTime = new SimpleDateFormat("dd-MM-yyyy").format(time);

                        String body = oj.getString("body");
                        String cleanText = html2text(body);

                        List<String> postAnswers = new ArrayList<>();
                        Boolean is_answered = oj.getBoolean("is_answered");
                        if (getAnswers && is_answered) {
                            int answerNumber = oj.getInt("answer_count");
                            if (answerNumber != 0) {
                                JSONArray answers = oj.getJSONArray("answers");

                                for (int j = 0; j < answerNumber; j++) {
                                    JSONObject answerObj = answers.getJSONObject(j);
                                    String answer = answerObj.getString("body");
                                    postAnswers.add(new Cleanup().cleanText(html2text(answer)).replace("\n", "").replace("\r", "").replace(";", ""));
                                    if (!getAnswersWithThreshold) {
                                        cleanText = cleanText + " " + html2text(answer);
                                    }
                                }
                            }
                        }

                        cleanText = cleanText.replace("\n", "").replace("\r", "").replace(";", "");

                        double tfidf = getTFIDFScore(cleanText, features, docsArray, tfidfDocsVector, d, threshold);

                        double w2v = getWord2VecScore(cleanText, benchmarkSentences, input_vectors, model, w, numFeatures, threshold);

                        double score = (tfidf + w2v) / 2;

                        if (score <= threshold && !security && NSRs < numNSRs) {
                            if (!postAnswers.isEmpty() && getAnswersWithThreshold) {
                                for (int p = 0; p < postAnswers.size(); p++) {
                                    double tfidfAnswer = getTFIDFScore(postAnswers.get(p), features, docsArray, tfidfDocsVector, d, threshold);
                                    double w2vAnswer = getWord2VecScore(postAnswers.get(p), benchmarkSentences, input_vectors, model, w, numFeatures, threshold);

                                    double answerScore = (tfidfAnswer + w2vAnswer) / 2;
                                    if (answerScore <= threshold) {
                                        cleanText = cleanText + " " + postAnswers.get(p);
                                    }
                                }
                            }

                            builder.append(title + ";");
                            builder.append(cleanText + ";");
                            builder.append(id + ";");
                            builder.append(newTime);
                            if (appendScoreToCSV) {
                                builder.append(";" + score);
                            }
                            builder.append('\n');

                            NSRs++;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (hasMore) {
                page++;
            } else {
                break;
            }
        }
        pw.write(builder.toString());
        pw.close();
        System.out.println("done!");
    }

    // using the average of the highest tfidf and word2vec score over a set threshold
    public static void getSRsWithThreshold(String newFile, String benchmarkDataset, String terms, String word2vec, String site, String tags, double threshold, int numSRs, int numFeatures, boolean getAnswers, boolean getAnswersWithThreshold, boolean appendScoreToCSV) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InterruptedException {

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(newFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder = new StringBuilder();
        String columnNamesList = "";
        if (appendScoreToCSV) {
            columnNamesList = "Title;Description;Id;Date;cossim";
        } else {
            columnNamesList = "Title;Description;Id;Date";
        }
        builder.append(columnNamesList + "\n");

        TFIDFSimilarity d = new TFIDFSimilarity();
        List<String> features = d.getNumTermsFromFile(terms, 200);

        List<String[]> docsArray = d.getDocsArrayFromCsv(benchmarkDataset);
        List<double[]> tfidfDocsVector = d.tfIdfCalculator(docsArray, docsArray, features);

        Word2VecSimilarity w = new Word2VecSimilarity();

        Word2Vec model = w.getWord2Vec(word2vec);

        List<Collection<String>> benchmarkSentences = new ArrayList<>();
        w.getSentences(benchmarkDataset, benchmarkSentences);
        List<INDArray> input_vectors = new ArrayList<>();

        for (int m = 0; m < benchmarkSentences.size(); m++) {
            input_vectors.add(w.getVector(benchmarkSentences.get(m), model, numFeatures));
        }

        Boolean hasMore = true;
        int page = 1;
        int SRs = 0;
        while (SRs < numSRs) {
            HttpClient client = HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD).build())
                    .build();
            HttpGet request = new HttpGet("https://api.stackexchange.com/2.2/questions?page=" + page + "&pagesize=100&order=desc&sort=activity&tagged=" + URLEncoder.encode(tags, "UTF-8") + "&site=" + site + "&filter=!--1nZwT3Ejsm&key=IT8vJtd)vD02vi1lzs5mHg((");

            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();

                // Read the contents of an entity and return it as a String.
                String content = EntityUtils.toString(entity);

                JSONObject result = new JSONObject(content);

                try {
                    hasMore = result.getBoolean("has_more");
                } catch (Exception e) {
                }

                JSONArray tokenList = new JSONArray();
                try {
                    tokenList = result.getJSONArray("items");
                } catch (Exception e) {
                }
                if (!tokenList.isEmpty()) {
                    for (int i = 0; i < tokenList.length(); i++) {
                        JSONObject oj = tokenList.getJSONObject(i);

                        if (SRs < numSRs) {
                            String title = oj.getString("title");
                            title = title.replace(";", "");

                            int id = oj.getInt("question_id");
                            int date = oj.getInt("creation_date");
                            Date time = new Date((long) date * 1000);

                            String newTime = new SimpleDateFormat("dd-MM-yyyy").format(time);

                            String body = oj.getString("body");
                            String cleanText = html2text(body);

                            List<String> postAnswers = new ArrayList<>();
                            Boolean is_answered = oj.getBoolean("is_answered");
                            if (getAnswers && is_answered) {
                                int answerNumber = oj.getInt("answer_count");
                                if (answerNumber != 0) {
                                    JSONArray answers = oj.getJSONArray("answers");

                                    for (int j = 0; j < answerNumber; j++) {
                                        JSONObject answerObj = answers.getJSONObject(j);
                                        String answer = answerObj.getString("body");
                                        postAnswers.add(new Cleanup().cleanText(html2text(answer)).replace("\n", "").replace("\r", "").replace(";", ""));
                                        if (!getAnswersWithThreshold) {
                                            cleanText = cleanText + " " + html2text(answer);
                                        }
                                    }
                                }
                            }

                            cleanText = cleanText.replace("\n", "").replace("\r", "").replace(";", "");

                            double tfidf = getTFIDFScore(cleanText, features, docsArray, tfidfDocsVector, d, threshold);

                            double w2v = getWord2VecScore(cleanText, benchmarkSentences, input_vectors, model, w, numFeatures, threshold);

                            double score = (tfidf + w2v) / 2;

                            if (score >= threshold && SRs < numSRs) {
                                if (!postAnswers.isEmpty() && getAnswersWithThreshold) {
                                    for (int p = 0; p < postAnswers.size(); p++) {
                                        double tfidfAnswer = getTFIDFScore(postAnswers.get(p), features, docsArray, tfidfDocsVector, d, threshold);
                                        double w2vAnswer = getWord2VecScore(postAnswers.get(p), benchmarkSentences, input_vectors, model, w, numFeatures, threshold);

                                        double answerScore = (tfidfAnswer + w2vAnswer) / 2;
                                        if (answerScore >= threshold) {
                                            cleanText = cleanText + " " + postAnswers.get(p);
                                        }
                                    }
                                }

                                builder.append(title + ";");
                                builder.append(cleanText + ";");
                                builder.append(id + ";");
                                builder.append(newTime);
                                if (appendScoreToCSV) {
                                    builder.append(";" + score);
                                }
                                builder.append('\n');

                                SRs++;
                                System.out.println(SRs);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (hasMore) {
                page++;
            } else {
                break;
            }
        }
        pw.write(builder.toString());
        pw.close();
        System.out.println("done!");
    }

    public static void getNSRsWithThresholdTFIDF(String newFile, String benchmarkDataset, String terms, String site, double threshold, int numNSRs, int numFeatures, boolean getAnswers, boolean getAnswersWithThreshold, boolean appendScoreToCSV) throws IOException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(newFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder = new StringBuilder();
        String columnNamesList = "";
        if (appendScoreToCSV) {
            columnNamesList = "Title;Description;Id;Date;cossim";
        } else {
            columnNamesList = "Title;Description;Id;Date";
        }
        builder.append(columnNamesList + "\n");

        TFIDFSimilarity d = new TFIDFSimilarity();

        List<String> features = d.getNumTermsFromFile(terms, 200);

        List<String[]> docsArray = d.getDocsArrayFromCsv(benchmarkDataset);
        List<double[]> tfidfDocsVector = d.tfIdfCalculator(docsArray, docsArray, features);

        Boolean hasMore = true;
        int page = 1;
        int NSRs = 0;
        while (NSRs < numNSRs) {
            HttpClient client = HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD).build())
                    .build();
            HttpGet request = new HttpGet("https://api.stackexchange.com/2.2/questions?page=" + page + "&pagesize=100&order=desc&sort=activity&&site=" + site + "&filter=!--1nZwT3Ejsm&key=IT8vJtd)vD02vi1lzs5mHg((");

            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();

                // Read the contents of an entity and return it as a String.
                String content = EntityUtils.toString(entity);

                JSONObject result = new JSONObject(content);

                try {
                    hasMore = result.getBoolean("has_more");
                } catch (Exception e) {
                    hasMore = false;
                }

                JSONArray tokenList = new JSONArray();
                try {
                    tokenList = result.getJSONArray("items");
                } catch (Exception e) {
                    break;
                }
                for (int i = 0; i < tokenList.length(); i++) {
                    JSONObject oj = tokenList.getJSONObject(i);

                    boolean security = false;

                    JSONArray tags = oj.getJSONArray("tags");
                    for (int j = 0; j < tags.length(); j++) {
                        if (tags.get(j).toString().contains("security")) {
                            security = true;
                            break;
                        }
                    }

                    if (!security && NSRs < numNSRs) {
                        String title = oj.getString("title");
                        title = title.replace(";", "");

                        int id = oj.getInt("question_id");
                        int date = oj.getInt("creation_date");
                        Date time = new Date((long) date * 1000);

                        String newTime = new SimpleDateFormat("dd-MM-yyyy").format(time);

                        String body = oj.getString("body");
                        String cleanText = html2text(body);

                        List<String> postAnswers = new ArrayList<>();
                        Boolean is_answered = oj.getBoolean("is_answered");
                        if (getAnswers && is_answered) {
                            int answerNumber = oj.getInt("answer_count");
                            if (answerNumber != 0) {
                                JSONArray answers = oj.getJSONArray("answers");

                                for (int j = 0; j < answerNumber; j++) {
                                    JSONObject answerObj = answers.getJSONObject(j);
                                    String answer = answerObj.getString("body");
                                    postAnswers.add(new Cleanup().cleanText(html2text(answer)).replace("\n", "").replace("\r", "").replace(";", ""));
                                    if (!getAnswersWithThreshold) {
                                        cleanText = cleanText + " " + html2text(answer);
                                    }
                                }
                            }
                        }

                        cleanText = cleanText.replace("\n", "").replace("\r", "").replace(";", "");

                        double score = getTFIDFScore(cleanText, features, docsArray, tfidfDocsVector, d, threshold);

                        if (score <= threshold && NSRs < numNSRs) {
                            if (!postAnswers.isEmpty() && getAnswersWithThreshold) {
                                for (int p = 0; p < postAnswers.size(); p++) {
                                    double answerScore = getTFIDFScore(postAnswers.get(p), features, docsArray, tfidfDocsVector, d, threshold);

                                    if (answerScore <= threshold) {
                                        cleanText = cleanText + " " + postAnswers.get(p);
                                    }
                                }
                            }

                            builder.append(title + ";");
                            builder.append(cleanText + ";");
                            builder.append(id + ";");
                            builder.append(newTime);
                            if (appendScoreToCSV) {
                                builder.append(";" + score);
                            }
                            builder.append('\n');

                            NSRs++;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (hasMore) {
                page++;
            } else {
                break;
            }
        }
        pw.write(builder.toString());
        pw.close();
        System.out.println("done!");
    }

    public static void getSRsWithThresholdTFIDF(String newFile, String benchmarkDataset, String terms, String site, String tags, double threshold, int numSRs, int numFeatures, boolean getAnswers, boolean getAnswersWithThreshold, boolean appendScoreToCSV) throws IOException, InterruptedException {

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(newFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder = new StringBuilder();
        String columnNamesList = "";
        if (appendScoreToCSV) {
            columnNamesList = "Title;Description;Id;Date;cossim";
        } else {
            columnNamesList = "Title;Description;Id;Date";
        }
        builder.append(columnNamesList + "\n");

        TFIDFSimilarity d = new TFIDFSimilarity();
        List<String> features = d.getNumTermsFromFile(terms, 200);

        List<String[]> docsArray = d.getDocsArrayFromCsv(benchmarkDataset);
        List<double[]> tfidfDocsVector = d.tfIdfCalculator(docsArray, docsArray, features);

        Boolean hasMore = true;
        int page = 1;
        int SRs = 0;
        while (SRs < numSRs) {
            HttpClient client = HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD).build())
                    .build();
            HttpGet request = new HttpGet("https://api.stackexchange.com/2.2/questions?page=" + page + "&pagesize=100&order=desc&sort=activity&tagged=" + URLEncoder.encode(tags, "UTF-8") + "&site=" + site + "&filter=!--1nZwT3Ejsm&key=IT8vJtd)vD02vi1lzs5mHg((");

            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();

                // Read the contents of an entity and return it as a String.
                String content = EntityUtils.toString(entity);

                JSONObject result = new JSONObject(content);

                try {
                    hasMore = result.getBoolean("has_more");
                } catch (Exception e) {
                }

                JSONArray tokenList = new JSONArray();
                try {
                    tokenList = result.getJSONArray("items");
                } catch (Exception e) {
                }
                if (!tokenList.isEmpty()) {
                    for (int i = 0; i < tokenList.length(); i++) {
                        JSONObject oj = tokenList.getJSONObject(i);

                        if (SRs < numSRs) {
                            String title = oj.getString("title");
                            title = title.replace(";", "");

                            int id = oj.getInt("question_id");
                            int date = oj.getInt("creation_date");
                            Date time = new Date((long) date * 1000);

                            String newTime = new SimpleDateFormat("dd-MM-yyyy").format(time);

                            String body = oj.getString("body");
                            String cleanText = html2text(body);

                            List<String> postAnswers = new ArrayList<>();
                            Boolean is_answered = oj.getBoolean("is_answered");
                            if (getAnswers && is_answered) {
                                int answerNumber = oj.getInt("answer_count");
                                if (answerNumber != 0) {
                                    JSONArray answers = oj.getJSONArray("answers");

                                    for (int j = 0; j < answerNumber; j++) {
                                        JSONObject answerObj = answers.getJSONObject(j);
                                        String answer = answerObj.getString("body");
                                        postAnswers.add(new Cleanup().cleanText(html2text(answer)).replace("\n", "").replace("\r", "").replace(";", ""));
                                        if (!getAnswersWithThreshold) {
                                            cleanText = cleanText + " " + html2text(answer);
                                        }
                                    }
                                }
                            }

                            cleanText = cleanText.replace("\n", "").replace("\r", "").replace(";", "");

                            double score = getTFIDFScore(cleanText, features, docsArray, tfidfDocsVector, d, threshold);

                            if (score >= threshold) {
                                if (!postAnswers.isEmpty() && getAnswersWithThreshold) {
                                    for (int p = 0; p < postAnswers.size(); p++) {
                                        double answerScore = getTFIDFScore(postAnswers.get(p), features, docsArray, tfidfDocsVector, d, threshold);

                                        if (answerScore >= threshold) {
                                            cleanText = cleanText + " " + postAnswers.get(p);
                                        }
                                    }
                                }

                                builder.append(title + ";");
                                builder.append(cleanText + ";");
                                builder.append(id + ";");
                                builder.append(newTime);
                                if (appendScoreToCSV) {
                                    builder.append(";" + score);
                                }
                                builder.append('\n');

                                SRs++;
                                System.out.println(SRs);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (hasMore) {
                page++;
            } else {
                break;
            }
        }
        pw.write(builder.toString());
        pw.close();
        System.out.println("done!");
    }

    public static void getNSRsWithThresholdWord2Vec(String newFile, String benchmarkDataset, String word2vec, String site, double threshold, int numNSRs, int numFeatures, boolean getAnswers, boolean getAnswersWithThreshold, boolean appendScoreToCSV) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(newFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder = new StringBuilder();
        String columnNamesList = "";
        if (appendScoreToCSV) {
            columnNamesList = "Title;Description;Id;Date;cossim";
        } else {
            columnNamesList = "Title;Description;Id;Date";
        }
        builder.append(columnNamesList + "\n");

        Word2VecSimilarity w = new Word2VecSimilarity();

        Word2Vec model = w.getWord2Vec(word2vec);

        List<Collection<String>> benchmarkSentences = new ArrayList<>();
        w.getSentences(benchmarkDataset, benchmarkSentences);
        List<INDArray> input_vectors = new ArrayList<>();

        for (int m = 0; m < benchmarkSentences.size(); m++) {
            input_vectors.add(w.getVector(benchmarkSentences.get(m), model, numFeatures));
        }

        Boolean hasMore = true;
        int page = 1;
        int NSRs = 0;
        while (NSRs < numNSRs) {
            HttpClient client = HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD).build())
                    .build();
            HttpGet request = new HttpGet("https://api.stackexchange.com/2.2/questions?page=" + page + "&pagesize=100&order=desc&sort=activity&&site=" + site + "&filter=!--1nZwT3Ejsm&key=IT8vJtd)vD02vi1lzs5mHg((");

            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();

                // Read the contents of an entity and return it as a String.
                String content = EntityUtils.toString(entity);

                JSONObject result = new JSONObject(content);

                try {
                    hasMore = result.getBoolean("has_more");
                } catch (Exception e) {
                    hasMore = false;
                }

                JSONArray tokenList = new JSONArray();
                try {
                    tokenList = result.getJSONArray("items");
                } catch (Exception e) {
                    break;
                }
                for (int i = 0; i < tokenList.length(); i++) {
                    JSONObject oj = tokenList.getJSONObject(i);

                    boolean security = false;

                    JSONArray tags = oj.getJSONArray("tags");
                    for (int j = 0; j < tags.length(); j++) {
                        if (tags.get(j).toString().equals("security")) {
                            security = true;
                            break;
                        }
                    }

                    if (!security && NSRs < numNSRs) {
                        String title = oj.getString("title");
                        title = title.replace(";", "");

                        int id = oj.getInt("question_id");
                        int date = oj.getInt("creation_date");
                        Date time = new Date((long) date * 1000);

                        String newTime = new SimpleDateFormat("dd-MM-yyyy").format(time);

                        String body = oj.getString("body");
                        String cleanText = html2text(body);

                        List<String> postAnswers = new ArrayList<>();
                        Boolean is_answered = oj.getBoolean("is_answered");
                        if (getAnswers && is_answered) {
                            int answerNumber = oj.getInt("answer_count");
                            if (answerNumber != 0) {
                                JSONArray answers = oj.getJSONArray("answers");

                                for (int j = 0; j < answerNumber; j++) {
                                    JSONObject answerObj = answers.getJSONObject(j);
                                    String answer = answerObj.getString("body");
                                    postAnswers.add(new Cleanup().cleanText(html2text(answer)).replace("\n", "").replace("\r", "").replace(";", ""));
                                    if (!getAnswersWithThreshold) {
                                        cleanText = cleanText + " " + html2text(answer);
                                    }
                                }
                            }
                        }

                        cleanText = cleanText.replace("\n", "").replace("\r", "").replace(";", "");

                        double score = getWord2VecScore(cleanText, benchmarkSentences, input_vectors, model, w, numFeatures, threshold);

                        if (score <= threshold && NSRs < numNSRs) {
                            if (!postAnswers.isEmpty() && getAnswersWithThreshold) {
                                for (int p = 0; p < postAnswers.size(); p++) {
                                    double answerScore = getWord2VecScore(postAnswers.get(p), benchmarkSentences, input_vectors, model, w, numFeatures, threshold);

                                    if (answerScore <= threshold) {
                                        cleanText = cleanText + " " + postAnswers.get(p);
                                    }
                                }
                            }

                            builder.append(title + ";");
                            builder.append(cleanText + ";");
                            builder.append(id + ";");
                            builder.append(newTime);
                            if (appendScoreToCSV) {
                                builder.append(";" + score);
                            }
                            builder.append('\n');

                            NSRs++;
                        }
                    }
                    security = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (hasMore) {
                page++;
            } else {
                break;
            }
        }
        pw.write(builder.toString());
        pw.close();
        System.out.println("done!");
    }

    public static void getSRsWithThresholdWord2Vec(String newFile, String benchmarkDataset, String word2vec, String site, String tags, double threshold, int numSRs, int numFeatures, boolean getAnswers, boolean getAnswersWithThreshold, boolean appendScoreToCSV) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InterruptedException {

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(newFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder = new StringBuilder();
        String columnNamesList = "";
        if (appendScoreToCSV) {
            columnNamesList = "Title;Description;Id;Date;cossim";
        } else {
            columnNamesList = "Title;Description;Id;Date";
        }
        builder.append(columnNamesList + "\n");

        Word2VecSimilarity w = new Word2VecSimilarity();

        Word2Vec model = w.getWord2Vec(word2vec);

        List<Collection<String>> benchmarkSentences = new ArrayList<>();
        w.getSentences(benchmarkDataset, benchmarkSentences);
        List<INDArray> input_vectors = new ArrayList<>();

        for (int m = 0; m < benchmarkSentences.size(); m++) {
            input_vectors.add(w.getVector(benchmarkSentences.get(m), model, numFeatures));
        }

        Boolean hasMore = true;
        int page = 1;
        int SRs = 0;
        while (SRs < numSRs) {
            HttpClient client = HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD).build())
                    .build();
            HttpGet request = new HttpGet("https://api.stackexchange.com/2.2/questions?page=" + page + "&pagesize=100&order=desc&sort=activity&tagged=" + URLEncoder.encode(tags, "UTF-8") + "&site=" + site + "&filter=!--1nZwT3Ejsm&key=IT8vJtd)vD02vi1lzs5mHg((");

            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();

                // Read the contents of an entity and return it as a String.
                String content = EntityUtils.toString(entity);

                JSONObject result = new JSONObject(content);

                try {
                    hasMore = result.getBoolean("has_more");
                } catch (Exception e) {
                }

                JSONArray tokenList = new JSONArray();
                try {
                    tokenList = result.getJSONArray("items");
                } catch (Exception e) {
                }
                if (!tokenList.isEmpty()) {
                    for (int i = 0; i < tokenList.length(); i++) {
                        JSONObject oj = tokenList.getJSONObject(i);

                        if (SRs < numSRs) {
                            String title = oj.getString("title");
                            title = title.replace(";", "");

                            int id = oj.getInt("question_id");
                            int date = oj.getInt("creation_date");
                            Date time = new Date((long) date * 1000);

                            String newTime = new SimpleDateFormat("dd-MM-yyyy").format(time);

                            String body = oj.getString("body");
                            String cleanText = html2text(body);
                            cleanText = cleanText.replace("\n", "").replace("\r", "").replace(";", "");

                            List<String> postAnswers = new ArrayList<>();
                            Boolean is_answered = oj.getBoolean("is_answered");
                            if (getAnswers && is_answered) {
                                int answerNumber = oj.getInt("answer_count");
                                if (answerNumber != 0) {
                                    JSONArray answers = oj.getJSONArray("answers");

                                    for (int j = 0; j < answerNumber; j++) {
                                        JSONObject answerObj = answers.getJSONObject(j);
                                        String answer = answerObj.getString("body");
                                        postAnswers.add(new Cleanup().cleanText(html2text(answer)).replace("\n", "").replace("\r", "").replace(";", ""));
                                        if (!getAnswersWithThreshold) {
                                            cleanText = cleanText + " " + html2text(answer);
                                        }
                                    }
                                }
                            }

                            double score = getWord2VecScore(cleanText, benchmarkSentences, input_vectors, model, w, numFeatures, threshold);

                            if (score >= threshold) {
                                if (!postAnswers.isEmpty() && getAnswersWithThreshold) {
                                    for (int p = 0; p < postAnswers.size(); p++) {
                                        double answerScore = getWord2VecScore(postAnswers.get(p), benchmarkSentences, input_vectors, model, w, numFeatures, threshold);

                                        if (answerScore >= threshold) {
                                            cleanText = cleanText + " " + postAnswers.get(p);
                                        }
                                    }
                                }

                                builder.append(title + ";");
                                builder.append(cleanText + ";");
                                builder.append(id + ";");
                                builder.append(newTime);
                                if (appendScoreToCSV) {
                                    builder.append(";" + score);
                                }
                                builder.append('\n');

                                SRs++;
                                System.out.println(SRs);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (hasMore) {
                page++;
            } else {
                break;
            }
        }
        pw.write(builder.toString());
        pw.close();
        System.out.println("done!");
    }

    public static void allMethods(String newFileTfidf, String newFileWord2Vec, String newFileBoth, String benchmarkDataset, String terms, String tfidfVectorsFile, String word2vec, String site, String tags, double threshold, int numSRs, int numFeatures, boolean getAnswers, boolean getAnswersWithThreshold) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InterruptedException {

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(newFileTfidf));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder = new StringBuilder();
        String columnNamesList = "Title;Description;Id;Date";
        builder.append(columnNamesList + "\n");

        PrintWriter pv = null;
        try {
            pv = new PrintWriter(new File(newFileWord2Vec));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder1 = new StringBuilder();
        builder1.append(columnNamesList + "\n");

        PrintWriter pd = null;
        try {
            pd = new PrintWriter(new File(newFileBoth));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder2 = new StringBuilder();
        builder2.append(columnNamesList + "\n");

        TFIDFSimilarity d = new TFIDFSimilarity();
        List<String> features = d.getNumTermsFromFile(terms, numFeatures);

        List<String[]> docsArray = d.getDocsArrayFromCsv(benchmarkDataset);
        List<double[]> tfidfDocsVector = d.getTFIDFVectorsFromFile(tfidfVectorsFile, numFeatures);
        //List<double[]> tfidfDocsVector = d.tfIdfCalculator(docsArray, docsArray, features);

        Word2VecSimilarity w = new Word2VecSimilarity();

        Word2Vec model = w.getWord2Vec(word2vec);

        List<Collection<String>> benchmarkSentences = new ArrayList<>();
        w.getSentences(benchmarkDataset, benchmarkSentences);
        List<INDArray> input_vectors = new ArrayList<>();

        for (int m = 0; m < benchmarkSentences.size(); m++) {
            input_vectors.add(w.getVector(benchmarkSentences.get(m), model, numFeatures));
        }

        Boolean hasMore = true;
        int page = 1;
        int SRsTfidf = 0;
        int SRsWord2Vec = 0;
        int SRsBoth = 0;
        int SRs = 0;
        while (SRs < numSRs) {
            HttpClient client = HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD).build())
                    .build();
            HttpGet request = new HttpGet("https://api.stackexchange.com/2.2/questions?page=" + page + "&pagesize=100&order=desc&sort=activity&tagged=" + URLEncoder.encode(tags, "UTF-8") + "&site=" + site + "&filter=!--1nZwT3Ejsm&key=IT8vJtd)vD02vi1lzs5mHg((");

            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();

                // Read the contents of an entity and return it as a String.
                String content = EntityUtils.toString(entity);
                //System.out.println(content);

                JSONObject result = new JSONObject(content);

                try {
                    hasMore = result.getBoolean("has_more");
                } catch (Exception e) {
                }

                JSONArray tokenList = new JSONArray();
                try {
                    tokenList = result.getJSONArray("items");
                } catch (Exception e) {
                }
                if (!tokenList.isEmpty()) {
                    for (int i = 0; i < tokenList.length(); i++) {
                        JSONObject oj = tokenList.getJSONObject(i);

                        if (SRs < numSRs) {
                            String title = oj.getString("title");
                            title = title.replace(";", "");

                            int id = oj.getInt("question_id");
                            int date = oj.getInt("creation_date");
                            Date time = new Date((long) date * 1000);

                            String newTime = new SimpleDateFormat("dd-MM-yyyy").format(time);

                            String body = oj.getString("body");
                            String cleanText = html2text(body);
                            cleanText = cleanText.replace("\n", "").replace("\r", "").replace(";", "");

                            List<String> postAnswers = new ArrayList<>();
                            Boolean is_answered = oj.getBoolean("is_answered");
                            if (getAnswers && is_answered) {
                                int answerNumber = oj.getInt("answer_count");
                                if (answerNumber != 0) {
                                    JSONArray answers = oj.getJSONArray("answers");

                                    for (int j = 0; j < answerNumber; j++) {
                                        JSONObject answerObj = answers.getJSONObject(j);
                                        String answer = answerObj.getString("body");
                                        postAnswers.add(new Cleanup().cleanText(html2text(answer)).replace("\n", "").replace("\r", "").replace(";", ""));
                                        if (!getAnswersWithThreshold) {
                                            cleanText = cleanText + " " + html2text(answer);
                                        }
                                    }
                                }
                            }

                            double tfidf = getTFIDFScore(cleanText, features, docsArray, tfidfDocsVector, d, threshold);

                            double w2v = getWord2VecScore(cleanText, benchmarkSentences, input_vectors, model, w, numFeatures, threshold);

                            double score = (tfidf + w2v) / 2;

                            if (tfidf >= threshold && SRsTfidf < numSRs) {
                                if (!postAnswers.isEmpty() && getAnswersWithThreshold) {
                                    for (int p = 0; p < postAnswers.size(); p++) {
                                        double answerScore = getTFIDFScore(postAnswers.get(p), features, docsArray, tfidfDocsVector, d, threshold);

                                        if (answerScore >= threshold) {
                                            cleanText = cleanText + " " + postAnswers.get(p);
                                        }
                                    }
                                }

                                builder.append(title + ";");
                                builder.append(cleanText + ";");
                                builder.append(id + ";");
                                builder.append(newTime);
                                builder.append('\n');

                                SRsTfidf++;
                            }

                            if (w2v >= threshold && SRsWord2Vec < numSRs) {
                                if (!postAnswers.isEmpty() && getAnswersWithThreshold) {
                                    for (int p = 0; p < postAnswers.size(); p++) {
                                        double answerScore = getWord2VecScore(postAnswers.get(p), benchmarkSentences, input_vectors, model, w, numFeatures, threshold);

                                        if (answerScore >= threshold) {
                                            cleanText = cleanText + " " + postAnswers.get(p);
                                        }
                                    }
                                }

                                builder1.append(title + ";");
                                builder1.append(cleanText + ";");
                                builder1.append(id + ";");
                                builder1.append(newTime);
                                builder1.append('\n');

                                SRsWord2Vec++;
                            }

                            if (score >= threshold && SRsBoth < numSRs) {
                                if (!postAnswers.isEmpty() && getAnswersWithThreshold) {
                                    for (int p = 0; p < postAnswers.size(); p++) {
                                        double tfidfAnswer = getTFIDFScore(postAnswers.get(p), features, docsArray, tfidfDocsVector, d, threshold);
                                        double w2vAnswer = getWord2VecScore(postAnswers.get(p), benchmarkSentences, input_vectors, model, w, numFeatures, threshold);

                                        double answerScore = (tfidfAnswer + w2vAnswer) / 2;
                                        if (answerScore >= threshold) {
                                            cleanText = cleanText + " " + postAnswers.get(p);
                                        }
                                    }
                                }

                                builder2.append(title + ";");
                                builder2.append(cleanText + ";");
                                builder2.append(id + ";");
                                builder2.append(newTime);
                                builder2.append('\n');

                                SRsBoth++;
                            }

                            if(SRsTfidf == numSRs && SRsWord2Vec == numSRs && SRsBoth == numSRs){
                                SRs = numSRs;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (hasMore) {
                page++;
            } else {
                break;
            }
        }
        pw.write(builder.toString());
        pw.close();
        pv.write(builder1.toString());
        pv.close();
        pd.write(builder2.toString());
        pd.close();
        System.out.println("done!");
    }

    private static double getTFIDFScore(String cleanText, List<String> features, List<String[]> docsArray, List<double[]> tfidfDocsVector, TFIDFSimilarity d, double threshold) {
        // check cosine similarity
        double[] cleanTextDoc = d.getDocumentVectors(cleanText, features, docsArray);

        double score = 0.0;
        double cosine = 0.0;
        for (int k = 0; k < tfidfDocsVector.size(); k++) {
            cosine = d.getCosineSimilarityTwoDocuments(cleanTextDoc, tfidfDocsVector.get(k));

            if (cosine > score) {
                score = cosine;
            }

            if (score >= threshold) {
                break;
            }
        }
        return score;
    }

    private static double getWord2VecScore(String cleanText, List<Collection<String>> benchmarkSentences, List<INDArray> input_vectors, Word2Vec model, Word2VecSimilarity w, int num_features, double threshold) {
        Collection<String> sentence = new Cleanup().normalizeText(cleanText);

        double score = 0.0;
        double cosine = 0.0;
        for (int k = 0; k < benchmarkSentences.size(); k++) {
            //INDArray input1_vector = w.getVector(benchmarkSentences.get(k), model);
            INDArray input2_vector = w.getVector(sentence, model, num_features);

            double dot_product = Nd4j.getBlasWrapper().dot(input_vectors.get(k), input2_vector);

            cosine = w.cosine_similarity(input_vectors.get(k).toDoubleVector(), input2_vector.toDoubleVector(), dot_product);

            if (cosine > score) {
                score = cosine;
            }

            if (score >= threshold) {
                break;
            }
        }
        return score;
    }

    public static String html2text(String html) {
        return Jsoup.parse(html).text();
    }
}