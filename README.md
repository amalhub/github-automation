# github-automation

The following project has been made to automate the process of moving Jira tickets to Github issues. In this approach I've used the Jira issue list export functionality to export the issue list into an XML file and then using the Github API, I have automated the process of pushing the issue list into Github.

#### 1. Exporting the issue list from Jira

   Once you have filtered the Jira tickets from the Jira browser, at the top right hand side corner there is a export button. Click on it and export the issue list into an XML file.
   
   ![Exporting Jira to XML](https://github.com/amalhub/github-automation/blob/master/readme_images/jira_export.png "Exporting Jira to XML")

#### 2. Running the script
  * Download this project into your machine and build it using Maven.
  * Now you need to update the configuration setting in the **ImportXmltoGit.java** file before running the program.
    * xmlFilePath
    * gitUrl => [https://api.github.com/repos/**:user**/**:repo**/issues](https://api.github.com/repos/:user/:repo/issues)
    * gitAuthToken
       * **Note:** To generate a github token goto: [https://github.com/settings/tokens](https://github.com/settings/tokens) 
       (Might need an admin token)
    * jiraUrl
    * additionalLabels (optional)
  * After updating the configurations run the script using Java.

#### 3. Updating the Jira issues
  If you want to perform a bulk operation on the moved Jira issues, refer [https://confluence.atlassian.com/jiracoreserver073/editing-multiple-issues-at-the-same-time-861257342.html](Editing multiple Jira issues at the same time). Using this functionality you can add a comment in the Jira directing the user to the Github issue list (Since the Git issue header contains the Jira issue ID, user can easily search for it). 

    **Example comment:** *"Moved to Github issues: ${link}. Search with Jira issue ID to find it in Git."*
