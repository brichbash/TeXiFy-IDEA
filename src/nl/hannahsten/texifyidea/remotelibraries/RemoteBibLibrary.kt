package nl.hannahsten.texifyidea.remotelibraries

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import io.ktor.client.statement.*
import nl.hannahsten.texifyidea.RemoteLibraryRequestFailure
import nl.hannahsten.texifyidea.psi.BibtexEntry
import nl.hannahsten.texifyidea.remotelibraries.state.BibtexEntryListConverter

/**
 * Remote library with a unique [identifier].
 */
abstract class RemoteBibLibrary(open val identifier: String, open val displayName: String) {

    /**
     * Get the bib items from the remote library in bibtex format, then parse the bibtex to obtain all the bib entries.
     *
     * When the request has a non-OK status code, return a failure instead.
     */
    suspend fun getCollection(): Either<RemoteLibraryRequestFailure, List<BibtexEntry>> = either {
        val (response, body) = getBibtexString()

        ensure(response.status.value !in 200 until 300) {
            RemoteLibraryRequestFailure(displayName, response)
        }

        // Reading the dummy bib file needs to happen in a place where we have read access.
        runReadAction {
            BibtexEntryListConverter().fromString(body)
        }
    }

    fun showNotification(project: Project, libraryName: String, response: HttpResponse) {
        val title = "Could not connect to $libraryName"
        val statusMessage = "${response.status.value}: ${response.status.description}"
        Notification("LaTeX", title, statusMessage, NotificationType.ERROR).notify(project)
    }

    /**
     * Get the bib items from the remote library in bibtex format.
     */
    abstract suspend fun getBibtexString(): Pair<HttpResponse, String>

    /**
     * Remove any credentials from the password safe.
     *
     * Use `PasswordSafe.instance.set(key, null)` to remove credentials for `key` from the password safe.
     */
    abstract fun destroyCredentials()
}