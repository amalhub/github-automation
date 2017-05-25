/*
 * Copyright (c) 2017, Amal Gunatilake All Rights Reserved.
 */
package org.amalhub.script;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class ImportXmltoGit {
    //Update these configurations according to your setup.
    private static final String xmlFilePath = "/home/daag/Downloads/CDMF_JiraList.xml";
    private static final String gitUrl = "https://api.github.com/repos/amalhub/test/issues";
    private static final String gitAuthToken = "88f0a72392f1fadee2e18d32db7dc10076d526a4";
    private static final String jiraUrl = "https://wso2.org/jira/browse/";

    public static void main(String[] args) {
        String title = "";
        int counter = 0;
        try {
            File xmlFile = new File(xmlFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
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
                    }
                    if (node.getNodeName().toString().equals("description")) {
                        description = node.getTextContent();
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
                addGitIssue(title, description, label);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            System.out.println("Creating issue failed: " + title);
            System.out.println("Issue counter id: " + counter);
            e.printStackTrace();
        }
    }

    private static void addGitIssue(String title, String description, String label) throws IOException {
        String payload = "{\n" +
                "  \"title\": \"" + title + "\",\n" +
                "  \"body\": \"" + description + "\",\n" +
                "  \"labels\": [\n" +
                "    \"" + label + "\"\n" +
                "  ]\n" +
                "}";
        System.out.println(payload);

        HttpPost httpPost = null;
        CloseableHttpResponse response;
        try {
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            httpPost = new HttpPost(gitUrl);

            httpPost.setEntity(new StringEntity(payload));

            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("User-Agent", "Doctrine Jira Migration");
            httpPost.addHeader("Authorization", "token " + gitAuthToken);

            response = httpClient.execute(httpPost);

            System.out.println("Status of adding to git: " + response.getStatusLine().getStatusCode() + "\n");
        } catch (IOException e) {
            throw e;
        } finally {
            if (httpPost != null) {
                httpPost.releaseConnection();
            }
        }

    }
}
