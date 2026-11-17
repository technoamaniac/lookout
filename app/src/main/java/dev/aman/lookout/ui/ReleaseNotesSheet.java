package dev.aman.lookout.ui;

import android.text.Html;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import dev.aman.lookout.R;
import dev.aman.lookout.update.Release;
import dev.aman.lookout.update.ReleaseNotesSource;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;

/** The sheet over the Updates tab that shows what a release changes, read from GitHub when it opens. */
final class ReleaseNotesSheet {
    private final AppCompatActivity activity;
    private final Executor background;
    private final ReleaseNotesSource source;
    private final Sheet sheet;
    private final TextView title;
    private final ScrollView scroll;
    private final TextView text;

    // Tells a reply for the sheet that is open from one for a sheet already closed and reopened
    private int opening;

    ReleaseNotesSheet(AppCompatActivity activity, Executor background, ReleaseNotesSource source) {
        this.activity = activity;
        this.background = background;
        this.source = source;
        sheet = new Sheet(activity, R.id.notes_overlay);
        title = activity.findViewById(R.id.notes_title);
        scroll = activity.findViewById(R.id.notes_scroll);
        text = activity.findViewById(R.id.notes_text);
        activity.findViewById(R.id.notes_close).setOnClickListener(v -> sheet.hide());
    }

    /** @param onClosed runs after the sheet closes, to hand focus back to where it came from */
    void show(Release release, Runnable onClosed) {
        int thisOpening = ++opening;
        title.setText(activity.getString(R.string.release_notes_title, release.version()));
        text.setText(R.string.release_notes_loading);
        // The sheet is reused, so start each release's notes at their top
        scroll.scrollTo(0, 0);
        sheet.show(scroll, onClosed);
        background.execute(() -> {
            CharSequence notes = load(release);
            activity.runOnUiThread(() -> {
                if (!activity.isDestroyed() && thisOpening == opening) text.setText(notes);
            });
        });
    }

    // Runs in the background: reading the notes goes to the network
    private CharSequence load(Release release) {
        try {
            Optional<String> html = source.notesFor(release);
            return html.isPresent()
                    ? Html.fromHtml(html.get(), Html.FROM_HTML_MODE_LEGACY)
                    : activity.getText(R.string.release_notes_none);
        } catch (IOException | RuntimeException e) {
            return activity.getText(R.string.update_check_failed);
        }
    }
}
