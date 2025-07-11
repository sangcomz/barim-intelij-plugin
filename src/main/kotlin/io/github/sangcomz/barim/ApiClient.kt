package io.github.sangcomz.barim

import com.intellij.util.io.HttpRequests
import kotlinx.serialization.json.Json

class ApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val baseUrl = "https://barim-api.vercel.app/api"

    fun getProjects(token: String): ProjectsApiResponse {
        val url = "$baseUrl/projects"
        val responseJson = HttpRequests.request(url)
            .tuner { connection ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            .readString()
        return json.decodeFromString<ProjectsApiResponse>(responseJson)
    }

    fun getIssues(token: String, repo: String): ApiResponse {
        val url = "$baseUrl/issues?repo=$repo"
        val responseJson = HttpRequests.request(url)
            .tuner { connection ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            .readString()
        return json.decodeFromString<ApiResponse>(responseJson)
    }

    fun updateIssue(token: String, issueNumber: Int, updateData: IssueUpdateData): UpdateApiResponse {
        val url = "$baseUrl/issues/$issueNumber"
        val body = json.encodeToString(IssueUpdateData.serializer(), updateData)

        val responseJson = HttpRequests.post(url, "application/json")
            .tuner { connection ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            .connect { request ->
                request.write(body)
                request.readString()
            }

        return json.decodeFromString<UpdateApiResponse>(responseJson)
    }

    fun createIssue(token: String, issueData: IssueCreateData): Issue {
        val url = "$baseUrl/issues"
        val body = json.encodeToString(IssueCreateData.serializer(), issueData)

        val responseJson = HttpRequests.post(url, "application/json")
            .tuner { connection ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            .connect { request ->
                request.write(body)
                request.readString()
            }

        return json.decodeFromString<UpdateApiResponse>(responseJson).issue
    }

    fun addCommentToIssue(token: String, issueNumber: Int, commentData: CommentCreateData): Comment {
        val url = "$baseUrl/issues/$issueNumber/comments"
        val body = json.encodeToString(CommentCreateData.serializer(), commentData)

        val responseJson = HttpRequests.post(url, "application/json")
            .tuner { connection ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            .connect { request ->
                request.write(body)
                request.readString()
            }

        return json.decodeFromString<CommentApiResponse>(responseJson).comment
    }

    fun createProject(token: String, projectData: ProjectCreateData): ProjectLabel {
        val url = "$baseUrl/projects"
        val body = json.encodeToString(ProjectCreateData.serializer(), projectData)

        val responseJson = HttpRequests.post(url, "application/json")
            .tuner { connection ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            .connect { request ->
                request.write(body)
                request.readString()
            }

        return json.decodeFromString<ProjectCreateApiResponse>(responseJson).project
    }

    fun hasRepository(token: String, repoName:String): RepositoryApiResponse {
        val url = "$baseUrl/repos/$repoName"
        val responseJson = HttpRequests.request(url)
            .tuner { connection ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            .readString()

        return json.decodeFromString<RepositoryApiResponse>(responseJson)
    }

    fun getUserRepositories(token: String): RepositoriesApiResponse {
        val url = "$baseUrl/repos"
        val responseJson = HttpRequests.request(url)
            .tuner { connection ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            .readString()

        return json.decodeFromString<RepositoriesApiResponse>(responseJson)
    }
}