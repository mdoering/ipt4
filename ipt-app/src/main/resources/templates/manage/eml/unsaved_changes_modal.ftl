<div id="unsavedChangesModal" class="modal fade" tabindex="-1" aria-labelledby="unsavedChangesModal-title" aria-hidden="true">
    <div class="modal-dialog modal-confirm modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header flex-column">
                <img src="${baseURL}/images/logo-modal-warning.png" alt="Warning" class="modal-image" />
            </div>

            <div class="modal-body">
                <h5 class="modal-title w-100" id="unsavedChangesModal-title">
                    ${t("manage.metadata.unsavedChanges.title")}<#t>
                </h5>

                <p>
                    ${t("manage.metadata.unsavedChanges.message")}<#t>
                </p>
            </div>

            <div class="modal-footer justify-content-center">
                <button id="stayButton" type="button" class="btn btn-sm btn-outline-gbif-primary" data-bs-dismiss="modal">
                    ${t("manage.metadata.unsavedChanges.button.stay")}<#t>
                </button>
                <button id="leaveButton" type="button" class="btn btn-sm btn-outline-gbif-danger" data-bs-dismiss="modal">
                    ${t("manage.metadata.unsavedChanges.button.leave")}<#t>
                </button>
            </div>
        </div>
    </div>
</div>