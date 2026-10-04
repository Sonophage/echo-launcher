package com.echo.feature.artwork.studio

import com.echo.core.domain.model.GamepadAction

interface ArtworkStudioActions {
    fun handleGamepadAction(action: GamepadAction)

    fun selectTab(index: Int)
    fun sourcesForTab(): List<StudioSource>
    fun selectSource(index: Int)
    fun openProviderPicker()
    fun requestLocalPick()
    fun toggleNsfw()

    fun openSearch()
    fun onQueryDraftChanged(text: String)
    fun submitSearch()
    fun cancelSearch()
    fun resetSearchToTitle()

    fun onChangeMatchPressed()
    fun onChangeMatchDraftChanged(text: String)
    fun startChangeMatchEdit()
    fun stopChangeMatchEdit()
    fun submitChangeMatch()
    fun confirmMatch(index: Int)
    fun cancelChangeMatch()
    fun forgetMatch()

    fun onGridMeasured(widthDp: Float, heightDp: Float)
    fun openCandidate(index: Int)

    fun pickAt(index: Int)
    fun toggleSelection(index: Int)
    fun previousPage()
    fun nextPage()

    fun applyChanges()
    fun closeReview()
    fun applyReviewed()

    fun resolveConfirm(index: Int)
    fun dismissConfirm()
    fun retryFailed()
    fun removeFailed()

    fun openAssetManager()
    fun closeAssetManager()
    fun focusManagedAsset(index: Int)
    fun moveManagedAsset(delta: Int)
    fun makeManagedAssetPrimary()
    fun resolveLeavePrompt(choice: StudioLeaveChoice)

    fun applyCandidate()
    fun dismissCandidate()
    fun onManualPageCount(count: Int)
    fun manualPreviousPage()
    fun manualNextPage()

    fun openActions()
    fun closeActions()
    fun runAction(action: StudioAction)
    fun panCrop(dx: Float, dy: Float)
    fun zoomCrop(factor: Float)
    fun applyCrop()
    fun cancelCrop()

    fun toggleCropPreview()

    fun openCropOptions()
    fun closeCropOptions()
    fun moveCropOptionsCursor(delta: Int)
    fun activateCropOption(index: Int)

    fun dismissMessage()
}
