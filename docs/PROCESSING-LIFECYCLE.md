# Processing lifecycle rules

These rules apply to every Audio Lab tool. They are product behavior, not just loading animations.

## 1. Entering a tool

- Open the tool screen and load its configuration.
- Restore the selected source and unsaved settings when the user leaves and reopens a tool.
- Validate supported input types and show the supported formats before the user starts.
- Do not begin destructive or expensive processing just because the screen opened.

## 2. Starting a job

- Require an explicit user action.
- Validate source URI access, file type, options and destination before enqueueing.
- For split jobs, validate the requested part count (2–20), WAV PCM format and frame availability before writing any published result.
- For multi-file join jobs, validate all sources use the same sample rate, channel count and bit depth.
- For reverse jobs, reverse whole PCM frames so left/right channel samples remain aligned.
- Submit a uniquely named WorkManager job with a tool tag.
- Disable repeated taps while submission is being confirmed.
- Show stages and progress from the worker rather than inventing progress in the UI.

## 3. Leaving a tool

- Navigating back does not cancel a running job.
- Navigating back does not enqueue a second job.
- The worker owns its input and temporary files for the duration of the operation.
- The processing queue remains available from the main navigation.
- The selected source remains available when reopening the same tool; opening a tool is setup only, never a new processing request.

## 4. Returning to the app

- Observe the existing WorkManager job by its processing tag and match it to the selected source URI.
- Display its real state: queued, running, succeeded, failed or cancelled.
- Keep independent files independent: a job for one source must not block or masquerade as the status of another source.
- Never restart a job automatically on Activity recreation, app resume or return navigation.
- Offer an explicit retry action only after a failed or completed job.

## 5. Cancellation and failure

- Cancellation must be initiated by the user from the queue or tool screen.
- Preserve the original input file.
- Delete incomplete output and temporary copies on failure or cancellation.
- Explain unsupported formats and validation errors in plain language.

## 6. Successful completion

- Publish a new output file; never overwrite the input.
- Return the output URI and technical summary to the queue.
- Keep the result discoverable after the user leaves the tool.
- A later operation is a new job and must be explicitly requested.
