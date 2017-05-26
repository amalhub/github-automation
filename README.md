# github-automation

The following project has been made to automate the process of moving Jira tickets to Github issues. In this approach I've used the Jira issue list export functionality to export the issue list into an XML file and then using the Github API, I have automated the process of pushing the issue list into Github.

#### 1. Exporting the issue list from Jira

   Once you have filtered the Jira tickets from the Jira browser, at the top right hand side corner there is a export button. Click on it and export the issue list into an XML file.
   
   ![Exporting Jira to XML](https://github.com/amalhub/github-automation/blob/master/readme_images/jira_export.png "Exporting Jira to XML")

#### 2. Running the script
  * Download this project into your machine and build it using Maven.
  * Now you need to update the configuration setting in the **ImportXmltoGit.java** file before running the program.
    * xmlFilePath
    * gitUrl => https://<span></span>api.github<span></span>.com/repos/**:user**/**:repo**/issues
    * gitAuthToken
       * **Note:** To generate a github token goto: [https://github.com/settings/tokens](https://github.com/settings/tokens) 
       (Might need an admin token)
    * jiraUrl
  * After updating the configurations run the script using Java.
