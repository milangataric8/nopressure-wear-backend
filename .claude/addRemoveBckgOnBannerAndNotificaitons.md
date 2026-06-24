Let me add the remove-background checkbox to both the banner upload and the notification image upload, same pattern
as the product/auth images.
I need to see the current upload code in both files to slot it in correctly. But based on the established pattern, 
here's what to add to each.

AdminBanners
Add the state near your other useState hooks:

jsxconst [removeBgBanner, setRemoveBgBanner] = useState(false);

Add the checkbox above the banner upload button, and pass removeBgBanner to uploadImage:

<div className="flex items-center gap-3 mb-2">
    <label className="flex items-center gap-2 cursor-pointer">
        <input
            type="checkbox"
            checked={removeBgBanner}
            onChange={(e) => setRemoveBgBanner(e.target.checked)}
            className="w-3.5 h-3.5"
        />
        <span className="text-xs text-gray-500 uppercase tracking-wide">
            {t('admin.removeBackground')}
        </span>
    </label>
</div>

<label className="cursor-pointer block">
    <div className="border border-gray-300 text-center py-2 text-xs font-semibold uppercase tracking-wide text-black 
                    hover:bg-gray-50 transition-colors">
        {t('admin.uploadImage')}
    </div>
    <input
        type="file"
        accept=".jpg,.jpeg,.png,.gif,.webp,.bmp"
        className="hidden"
        onChange={async (e) => {
            const file = e.target.files[0];
            if (!file) return;
            e.target.value = '';
            try {
                const response = await uploadImage(file, removeBgBanner);
                // ... your existing logic that sets the banner image url
            } catch (err) {
                toast.error(err.response?.data?.message || t('messages.failedToUploadImage'));
            }
        }}
    />
</label>

AdminNotifications
You already have an image upload there. Add the state:

const [removeBgNotification, setRemoveBgNotification] = useState(false);
Add the checkbox above the image upload and pass the flag to uploadImage:
jsx<div className="flex items-center gap-3 mb-2">
    <label className="flex items-center gap-2 cursor-pointer">
        <input
            type="checkbox"
            checked={removeBgNotification}
            onChange={(e) => setRemoveBgNotification(e.target.checked)}
            className="w-3.5 h-3.5"
        />
        <span className="text-xs text-gray-500 uppercase tracking-wide">
            {t('admin.removeBackground')}
        </span>
    </label>
</div>

And in the existing upload handler, change uploadImage(file) to uploadImage(file, removeBgNotification):
const response = await uploadImage(file, removeBgNotification);

That's the pattern for both. The key in each case is: separate state per upload (removeBgBanner, removeBgNotification), 
a checkbox bound to it, and passing that flag as the second argument to uploadImage.

To give you the exact edits rather than the pattern, paste the current image-upload blocks from AdminBanners.jsx and
AdminNotifications.jsx and I'll drop the checkbox in at the precise spot with your actual handler logic. 
But if the pattern's clear, you can apply it directly — the only real wiring is uploadImage(file, removeBgX).