/*
 * Copyright (c) 2017, Amal Gunatilake All Rights Reserved.
 */
package org.amalhub.script;

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
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportXmltoGit {

    //Update these configurations according to your setup.
    private static final String xmlFilePath = ""; //Add path to the Jira xml
    private static final String repoName = ""; //Add your GitHub repo name here
    private static final String gitUrl = "https://api.github.com/repos/wso2/" + repoName + "/issues";

    private static final String gitAuthToken = ""; //Add your GitHub auth tokan here
    private static final String jiraUrl = "https://wso2.org/jira/browse/";

    private static final String jiraUser = ""; //Add your Jira username here
    private static final String jiraPass = ""; //Add your Jira password here
    private static List<String> failedIssuesGit = new ArrayList<>();

    private static List<String> failedIssuesJira = new ArrayList<>();

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
                    if (node.getNodeName().toString().equals("priority")) {
                        if (node.getTextContent().toLowerCase().contains("high")) {
                            label += ",high";
                        } else if (node.getTextContent().toLowerCase().contains("normal")) {
                            label += ",medium";
                        } else {
                            label += ",low";
                        }
                    }
                }
                description += "<p>Reference: <a href='" + url + "'>" + url + "</a></p>";
                description = description.replace("\n", "");
                description = description.replace("\t", "");
                System.out.println("Processing id:" + counter + " => " + title + "\n");
                addGitIssue(title, description, label, url);
            }
            System.out.println("---------------------Issues Failed to move to Git----------------------");
            if (!failedIssuesGit.isEmpty()) {
                for (String issue : failedIssuesGit) {
                    System.out.println(issue);
                }
            } else {
                System.out.println("All issues moved to Git successfully \n\n");
            }
            System.out.println("---------------------Jira's that are not updated----------------------");
            if (!failedIssuesJira.isEmpty()) {
                for (String issue : failedIssuesJira) {
                    System.out.println(issue);
                }
            } else {
                System.out.println("All Jira's updated and closed successfully");
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            System.out.println("Creating issue failed: " + title);
            System.out.println("Issue counter id: " + counter);
            e.printStackTrace();
        }
    }

    private static String createValidXml(String xmlString) {
        xmlString = xmlString.replaceAll("&#91;", "[");
        xmlString = xmlString.replaceAll("&#93;", "]");
        xmlString = xmlString.replaceAll("&", "&amp;");
        String descTag = "<description>\n                ";
        Pattern xml = Pattern.compile(descTag);
        Matcher match = xml.matcher(xmlString);
        int spacing = 0;
        while (match.find()) {
            int endIndex = match.end();
            StringBuilder sb = new StringBuilder(xmlString);
            sb.insert(endIndex + spacing, "<![CDATA[");
            xmlString = sb.toString();
            spacing += 9;
        }
        descTag = "\n            </description>";
        xml = Pattern.compile(descTag);
        match = xml.matcher(xmlString);
        spacing = 0;
        while (match.find()) {
            int startIndex = match.start();
            StringBuilder sb = new StringBuilder(xmlString);
            sb.insert(startIndex + spacing, "]]>");
            xmlString = sb.toString();
            spacing += 3;
        }
        return xmlString;
    }

    private static void addGitIssue(String title, String description, String label, String jiraUrl)
            throws IOException {
        JSONObject payload = new JSONObject();
        payload.put("title", title);
        payload.put("body", description);
        String labels[] = label.split(",");
        payload.put("labels", labels);
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
            int code = response.getStatusLine().getStatusCode();
            System.out.println("Status of adding to git: " + code + "\n");
            if (code == 201) {
                String json = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = new JSONObject(json);
                String gitUrl = jsonObject.getString("url");
                gitUrl = gitUrl.replace("api.github.com/repos", "github.com");
                addJIRAComment(gitUrl, jiraUrl);
            } else {
                failedIssuesGit.add(title);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (httpPost != null) {
                httpPost.releaseConnection();

            }
        }
    }

    private static void addJIRAComment(String gitUrl, String jiraUrl) {
        String requestUrl = "https://wso2.org/jira/rest/api/2/issue/" + jiraUrl.replace(
                "https://wso2.org/jira/browse/", "") + "/transitions?expand=transitions.fields";
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
        System.out.println(payload.toString());
        HttpPost httpPost = null;
        CloseableHttpResponse response;
        try {
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            httpPost = new HttpPost(requestUrl);
            httpPost.setEntity(new StringEntity(payload.toString()));
            String userpass = jiraUser + ":" + jiraPass;
            String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(
                    userpass.getBytes("UTF-8"));
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("Authorization", basicAuth);
            response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 204) {
                System.out.println(
                        "Status of closing Jira " + response.getStatusLine().getStatusCode() + "\n\n");
            } else {
                System.out.println(
                        "Jira issue closing failed: " + response.getStatusLine().getStatusCode() + "\n\n");
                failedIssuesJira.add(jiraUrl);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
