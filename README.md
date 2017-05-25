# github-automation

The following project has been made to automate the process of moving Jira tickets to Github issues. In this approach I have used the Jira issue list export functionality to export the issue list into an XML fine and then using the Github API automate the process of pushing the issue list into Github.

#### 1. Exproting the issue list from Jira

   Once you filter the Jira tickets from the Jira browser, at the top right hand side corner there is a export button. Click on it and export the issue list into an XML file.

#### 2. Running the script
  * Download this project into your machine and build it using Maven.
  * Now you need to update the configuration setting in the ImportXmltoGit.java file before running the program.
    * xmlFilePath
    * gitUrl
    * gitAuthToken
       * **Note:** To generate a github token goto [https://github.com/settings/tokens](https://github.com/settings/tokens) 
    * jiraUrl
  * Now run the script using Java.
