package io.github.sangcomz.barim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    val issues: List<Issue>,
    val meta: Meta
)
@Serializable
data class Issue(
    val id: Long,
    val number: Int,
    val title: String,
    val state: String,
    val body: String?,
    val user: User,
    val labels: List<Label>,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("closed_at") val closedAt: String? = null
)
@Serializable
data class Label(
    val name: String,
    val color: String
)
@Serializable
data class User(
    val login: String
)
@Serializable
data class Meta(
    val totalCount: Int? = null,
    val page: Int? = null,
    val projectLabel: String,
    val owner: String
)

@Serializable
data class IssueUpdateData(
    val title: String? = null,
    val body: String? = null,
    val labels: List<String>? = null,
    val state: String? = null,
    @SerialName("state_reason") val stateReason: String? = null
)
@Serializable
data class UpdateApiResponse(
    val issue: Issue
)
@Serializable
data class GithubRepo(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String
)
@Serializable
data class IssueCreateData(
    val title: String,
    val body: String,
    val issueType: String,
    val repo: String,
    val tags: List<String>? = null
)
@Serializable
data class CommentCreateData(
    val body: String
)
@Serializable
data class Comment(
    val id: Long,
    val body: String?,
    val user: User
)
@Serializable
data class CommentApiResponse(
    val comment: Comment
)

@Serializable
data class ProjectLabel(
    val name: String,
    val label: String,
    val color: String,
    val description: String? = null,
    val issueCount: Int
)

@Serializable
data class ProjectsApiResponse(
    val projects: List<ProjectLabel>
)

@Serializable
data class ProjectCreateData(
    val projectName: String
)

@Serializable
data class ProjectCreateApiResponse(
    val project: ProjectLabel
)

@Serializable
data class RepositoriesApiResponse(
    val repositories: List<GithubRepo>
)

@Serializable
data class RepositoryApiResponse(
    val repository: GithubRepo
)