/*
 * Copyright (c) 2017, Amal Gunatilake All Rights Reserved.
 */
package org.amalhub.script;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportXmltoGit {
    //Update these configurations according to your setup.
    private static final String xmlFilePath = "CDMF_JiraList.xml";
    private static final String gitUrl = "https://api.github.com/repos/madhawap/test/issues";
    private static final String gitAuthToken = "";
    private static final String jiraUrl = "https://wso2.org/jira/browse/";

    private static final String jiraUser = "";
    private static final String jiraPass = "";

    public static void main(String[] args) {
        String title = "";
        int counter = 0;
        try {
            File xmlFile = new File(xmlFilePath);
            FileInputStream fis = new FileInputStream(xmlFile);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int) xmlFile.length()];
            dis.readFully(keyBytes);
            dis.close();
            String xmlString = new String(keyBytes);
            xmlString = createValidXml(xmlString);

            DocumentBuilder newDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = newDocumentBuilder.parse(new ByteArrayInputStream(xmlString.getBytes()));
            doc.getDocumentElement().normalize();

            NodeList itemList = doc.getElementsByTagName("item");
            for (int i = 0; i < itemList.getLength(); i++) {
                counter = i;
                Node item = itemList.item(i);
                NodeList contentList = item.getChildNodes();
                title = "";
                String description = "";
                String url = jiraUrl;
                String label = "";
                for (int j = 0; j < contentList.getLength(); j++) {
                    Node node = contentList.item(j);
                    if (node.getNodeName().toString().equals("title")) {
                        title = node.getTextContent();
                        title = title.trim();
                    }
                    if (node.getNodeName().toString().equals("description")) {
                        description = node.getTextContent();
                        description = description.trim();
                        description += "\n";
                    }
                    if (node.getNodeName().toString().equals("key")) {
                        url += node.getTextContent();
                    }
                    if (node.getNodeName().toString().equals("type")) {
                        if (node.getTextContent().toLowerCase().contains("bug")) {
                            label = "bug";
                        } else {
                            label = "enhancement";
                        }
                    }
                }
                description += "<p>Reference: <a href='" + url + "'>" + url + "</a></p>";
                description = description.replace("\n", "");
                description = description.replace("\t", "");
                System.out.println("Processing id:" + counter + " => " + title + "\n");
                addGitIssue(title, description, label, url);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            System.out.println("Creating issue failed: " + title);
            System.out.println("Issue counter id: " + counter);
            e.printStackTrace();
        }
    }

    private static String createValidXml(String xmlString) {
        xmlString = xmlString.replaceAll("&", "&amp;");

        String descTag = "<description>\n                ";
        Pattern xml = Pattern.compile(descTag);
        Matcher match = xml.matcher(xmlString);

        int startIndex, endIndex;

        while (match.find()) {
            endIndex = match.end();

            StringBuilder sb = new StringBuilder(xmlString);
            sb.insert(endIndex, "<![CDATA[");
            xmlString = sb.toString();
        }

        descTag = "\n            </description>";
        xml = Pattern.compile(descTag);
        match = xml.matcher(xmlString);
        while (match.find()) {
            startIndex = match.start();

            StringBuilder sb = new StringBuilder(xmlString);
            sb.insert(startIndex, "]]>");
            xmlString = sb.toString();
        }
        return xmlString;
    }

    private static void addGitIssue(String title, String description, String label, String jiraUrl) throws IOException {
        JSONObject payload = new JSONObject();
        payload.put("title", title);
        payload.put("body", description);
        JSONArray array = new JSONArray();
        array.put(label);
        payload.put("labels", array);
        System.out.println(payload.toString());


        HttpPost httpPost = null;
        CloseableHttpResponse response;
        try {
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            httpPost = new HttpPost(gitUrl);

            httpPost.setEntity(new StringEntity(payload.toString()));

            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("User-Agent", "Doctrine Jira Migration");
            httpPost.addHeader("Authorization", "token " + gitAuthToken);

            response = httpClient.execute(httpPost);

            System.out.println("Status of adding to git: " + response.getStatusLine().getStatusCode() + "\n");

            String json = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = new JSONObject(json);
            String gitUrl = jsonObject.getString("url");
            gitUrl = gitUrl.replace("api.github.com/repos", "github.com");
            addJIRAComment(gitUrl, jiraUrl);
        } catch (IOException e) {
            throw e;
        } finally {
            if (httpPost != null) {
                httpPost.releaseConnection();
            }
        }
    }

    private static void addJIRAComment(String gitUrl, String jiraUrl) {
        String requestUrl = "https://wso2.org/jira/rest/api/2/issue/" + jiraUrl.replace("https://wso2.org/jira/browse/", "") +
                "/transitions?expand=transitions.fields";
        JSONObject payload = new JSONObject();

        JSONObject update = new JSONObject();
        JSONArray comment = new JSONArray();
        JSONObject add = new JSONObject();
        add.put("body", "This issue is moved to " + gitUrl + ", thus closed.");
        comment.put(new JSONObject().put("add", add));
        update.put("comment", comment);
        payload.put("update", update);

        JSONObject fields = new JSONObject();
        JSONObject resolution = new JSONObject();
        resolution.put("name", "Duplicate");
        fields.put("resolution", resolution);
        payload.put("fields", fields);

        JSONObject transition = new JSONObject();
        transition.put("id", "2");
        payload.put("transition", transition);

        System.out.print(payload.toString());

        HttpPost httpPost = null;
        CloseableHttpResponse response;
        try {
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            httpPost = new HttpPost(requestUrl);
            httpPost.setEntity(new StringEntity(payload.toString()));

            String userpass = jiraUser + ":" + jiraPass;
            String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes("UTF-8"));
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("Authorization", basicAuth);
            response = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity());
            System.out.println(responseString);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
