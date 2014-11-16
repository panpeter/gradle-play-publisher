package de.triplet.gradle.play

import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ApkListing
import com.google.api.services.androidpublisher.model.Track
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

class PlayPublishApkTask extends PlayPublishTask {

    static def MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT = 500
    static def FILE_NAME_FOR_WHATS_NEW_TEXT = "whatsnew"

    @Input
    File apkFile

    @InputDirectory
    File inputFolder

    @TaskAction
    publishApk() {
        super.publish()

        FileContent newApkFile = new FileContent(AndroidPublisherHelper.MIME_TYPE_APK, apkFile)
        Apk apk = edits.apks()
                .upload(applicationId, editId, newApkFile)
                .execute()

        Track newTrack = new Track().setVersionCodes([apk.getVersionCode()])
        edits.tracks()
                .update(applicationId, editId, extension.track, newTrack)
                .execute()

        // Matches if locale have the correct naming e.g. en-US for play store
        inputFolder.eachDirMatch(matcher) { dir ->
            File whatsNewFile = new File(dir, FILE_NAME_FOR_WHATS_NEW_TEXT + "-" + extension.track)

            if (!whatsNewFile.exists()) {
                whatsNewFile = new File(dir, FILE_NAME_FOR_WHATS_NEW_TEXT)
            }

            if (whatsNewFile.exists()) {
                def whatsNewText = TaskHelper.readAndTrimFile(whatsNewFile, MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT)
                def locale = dir.getName()

                ApkListing newApkListing = new ApkListing().setRecentChanges(whatsNewText)
                edits.apklistings()
                        .update(applicationId, editId, apk.getVersionCode(), locale, newApkListing)
                        .execute()
            }
        }

        edits.commit(applicationId, editId).execute()
    }

}