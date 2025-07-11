[한국어](./README.ko.md)

# Barim

Barim is an IntelliJ plugin that helps you manage personal to-dos and ideas for your coding projects directly inside the IDE. All your data is securely stored as GitHub Issues in a dedicated `barim-data` repository under your own GitHub account.

You can also manage your tasks and notes on the go with the **[official web application](https://barim-app.vercel.app/)**.
***

## Key Features

* **IDE-Integrated Management**: Handle your tasks and notes without breaking your coding workflow.
* **GitHub-Powered**: Your GitHub account becomes your personal database. All data is synchronized as GitHub Issues, ensuring it's safe and accessible.
* **Kanban Board**: Systematically track your work with `TODO`, `IN PROGRESS`, `DONE`, and `PENDING` statuses.
* **Tagging System**: Add tags to your notes to easily categorize and find your ideas.

***

## Screenshot

| Tasks                               | Notes                               |
| ----------------------------------- | ----------------------------------- |
| ![Tasks Screen](image/task.png) | ![Notes Screen](image/note.png) |

## Prerequisites

Before you start using Barim, you **must** set up two things.

### 1. `barim-data` Repository

All your tasks and notes are stored in a specific repository in your GitHub account.

* **Repository Name**: It must be named exactly **`barim-data`**.
* **Visibility**: We strongly recommend creating it as a **Private** repository to keep your data confidential.

> 👉 **[Click here to create the `barim-data` repository now](https://github.com/new?name=barim-data&description=Data%20for%20the%20Barim%20IDE%20plugin.)**
> (This link pre-fills the repository name and description. Set it to Private and click create.)

### 2. GitHub Personal Access Token

The plugin needs a GitHub token to access your `barim-data` repository. For security, we recommend using a **Fine-grained personal access token** with minimal permissions.

When creating the token, configure the permissions as follows:

* **Repository access**: Select `Only select repositories` and choose the `barim-data` repository you just created.
* **Permissions**: Under **Repository permissions**, set the following:
  * `Issues`: **Read and Write** (Required)
  * `Metadata`: **Read-only** (Required)

![Token Permission Example](image/token_permission.png)

As shown in the screenshot, only permissions for **Issues** and **Metadata** are required. You do not need to grant any other permissions.

***

## 🚀 How to Use

1.  Install the **Barim** plugin from the IntelliJ **Marketplace**.
2.  Open the **Barim** tool window from the right-side panel of the IDE (or go to `View > Tool Windows > Barim`).
3.  Click the `Login with GitHub` button and authenticate using the **Personal Access Token** you created above.
4.  Once logged in, the plugin will automatically load your `barim-data` project.
5.  Click the `Add` button to create your first task or note!