Let me add the remove-background checkbox to both the banner upload and the notification image upload, 
same pattern as the product/auth images.

I need to see the current upload code in both files to slot it in correctly. But based on the established pattern,
here's what to add to each.

AdminBanners
Add the state near your other useState hooks:
const [removeBgBanner, setRemoveBgBanner] = useState(false);

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
    <div className="border border-gray-300 text-center py-2 text-xs font-semibold uppercase tracking-wide text-black hover:bg-gray-50 transition-colors">
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
<div className="flex items-center gap-3 mb-2">
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
Let me know if it works or if you want me to do the exact integration.You said: I want to add the text option, 
like for description to have bold, italic, bullet, and other options when writing a messages for popups and notifications

Popups

AdminPopups.jsx — replace the content textarea
import RichTextEditor from '../../components/common/RichTextEditor';

{/* Replace the content textarea */}
<div>
    <label className={labelClass}>{t('admin.popupContent')}</label>
    <RichTextEditor
        value={formData.content}
        onChange={(value) => setFormData(prev => ({ ...prev, content: value }))}
        placeholder={t('admin.popupContentPlaceholder')}
    />
</div>

HomePopup.jsx — render the HTML

Where the popup content displays, use dangerouslySetInnerHTML:
<div
    className="product-description text-sm text-gray-600"
    dangerouslySetInnerHTML={{ __html: popup.content }}
/>

(reuse your .product-description CSS so the bold/italic/bullets render with the styling you already have)

Live preview in AdminPopups

If your AdminPopups has a live preview of the popup, render the content there with dangerouslySetInnerHTML too:
<div
    className="product-description"
    dangerouslySetInnerHTML={{ __html: formData.content }}
/>

Notifications

AdminNotifications.jsx — replace the message textarea

import RichTextEditor from '../../components/common/RichTextEditor';

<div className="mb-6">
    <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1.5">
        {t('admin.message') || 'Message'}
    </label>
    <RichTextEditor
        value={message}
        onChange={setMessage}
        placeholder="Get 30% off all products this weekend!"
    />
</div>

Since message is a plain string state (const [message, setMessage] = useState('')), onChange={setMessage} works directly — 
TipTap passes the HTML string.
Backend — the email broadcast must render HTML, not escape it
This is the important part. Your broadcast email currently puts the message into the HTML. If it was a plain-text 
message before, you may have wrapped it in something that preserves line breaks (like white-space: pre-line).
Now that message is already HTML, insert it directly without escaping.

In EmailService.sendBroadcastEmail, the message goes straight into the template:
java String html = """
    <div style="font-family: ...; max-width: 560px; margin: 0 auto; padding: 40px 0;">
        %s
        <div style="...">
            <h1>...</h1>
        </div>
        <div class="broadcast-content" style="font-size: 14px; color: #333; line-height: 1.6;">
            %s
        </div>
    </div>
    """.formatted(imageHtml, subject, message);

The message is now rich HTML (<p>, <strong>, <ul>, etc.), so it renders formatted in the email. Remove any white-space: 
pre-line since you no longer need manual line-break preservation — the HTML carries its own structure. Add some basic 
styling so bullets and headings render in the email:

java<style>
    .broadcast-content p { margin: 0 0 10px; }
    .broadcast-content ul { padding-left: 20px; margin: 0 0 10px; }
    .broadcast-content ol { padding-left: 20px; margin: 0 0 10px; }
    .broadcast-content strong { font-weight: 700; }
    .broadcast-content em { font-style: italic; }
    .broadcast-content h1, .broadcast-content h2, .broadcast-content h3 { margin: 0 0 8px; }
</style>

(email clients vary in CSS support, but Gmail/Apple Mail/Mailtrap handle these inline-ish styles fine; for bulletproof 
rendering you'd inline every style, but this works for most)

Notification history display

In the broadcast history list, the message is now HTML. Render it:
<div
    className="text-xs text-gray-500 mb-2 line-clamp-2"
    dangerouslySetInnerHTML={{ __html: b.message }}
/>
(line-clamp-2 keeps the history preview compact)

A few things to watch:
WhatsApp/Viber don't support HTML. If you ever wire up those channels, they need plain text — you'd strip the HTML tags 
before sending (e.g. convert to plain text or markdown). Email is the only channel that renders the formatting. 
Since those are stubs for now, not urgent, but worth remembering.

Popup showOnce / storage stays the same — only the content rendering changes.

Database columns — popup.content and broadcast.message are likely already TEXT, so they hold HTML fine. No migration 
needed unless they're VARCHAR with a small limit; HTML is longer than plain text, so if they're VARCHAR(255) bump them to TEXT.

Check the popup content column type — if it's VARCHAR, change it:
sqlALTER TABLE popup ALTER COLUMN content TYPE TEXT;
ALTER TABLE broadcast ALTER COLUMN message TYPE TEXT;

Try the popup first (simpler, no email involved), confirm the editor saves and HomePopup renders the formatting, then do 
notifications. Let me know how it goes!You said: Also I want to have review option and options to change 
background and text colors like for popupsAlso I want to have review option and options to change background and text 
colors like for popups3:58 PMClaude responded: You want the notification composer to have the same extras the popups 
have — a live preview ("review") of how the message will look, plus background and text c…You want the notification 
composer to have the same extras the popups have — a live preview ("review") of how the message will look, plus 
background and text color pickers. Let me add those to AdminNotifications.

Backend

Flyway — add color columns to broadcast
sqlALTER TABLE broadcast ADD COLUMN bg_color VARCHAR(20) DEFAULT '#ffffff';
ALTER TABLE broadcast ADD COLUMN text_color VARCHAR(20) DEFAULT '#111111';

Broadcast.java
java@Column(name = "bg_color")
private String bgColor;

@Column(name = "text_color")
private String textColor;
BroadcastRequest.java
javaprivate String bgColor;
private String textColor;
NotificationService — save and pass the colors
When building the Broadcast record and sending, include the colors. In broadcast(...):
javaBroadcast broadcast = Broadcast.builder()
.subject(request.getSubject())
.message(request.getMessage())
.imageUrl(request.getImageUrl())
.bgColor(request.getBgColor())
.textColor(request.getTextColor())
.channels(String.join(", ", request.getChannels()))
.recipients(customers.size())
.sentBy(sentBy)
.build();
broadcastRepository.save(broadcast);
The email channel needs the colors too. Pass them through NotificationChannel.send(...) — but rather than expand that interface again, the simplest path is to bake the colors into the HTML inside EmailService.sendBroadcastEmail by passing them as parameters. Update the email method signature to accept bgColor and textColor and apply them:
javapublic void sendBroadcastEmail(String to, String subject, String message,
String imageUrl, String bgColor, String textColor) {
String bg = (bgColor != null && !bgColor.isBlank()) ? bgColor : "#ffffff";
String text = (textColor != null && !textColor.isBlank()) ? textColor : "#111111";

    // ... build imageHtml as before ...

    String html = """
        <div style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; max-width: 560px; margin: 0 auto; background: %s; padding: 40px;">
            %s
            <h1 style="font-size: 22px; font-weight: 800; text-transform: uppercase; margin: 0 0 20px; color: %s;">
                %s
            </h1>
            <div class="broadcast-content" style="font-size: 14px; line-height: 1.6; color: %s;">
                %s
            </div>
        </div>
        """.formatted(bg, imageHtml, text, subject, text, message);

    sendHtmlEmail(to, subject != null && !subject.isBlank() ? subject : "Special Offer", html);
}

(The EmailNotificationChannel calls this — pass the colors through from the service. Since the NotificationChannel.send 
interface doesn't carry colors, the cleanest fix is for NotificationService to call emailService.sendBroadcastEmail(...) 
directly for the email channel instead of going through the generic interface, OR widen the interface. Given you only 
have email live, calling EmailService directly for email is simplest.)

Frontend — AdminNotifications.jsx
Add state for colors:
const [bgColor, setBgColor] = useState('#ffffff');
const [textColor, setTextColor] = useState('#111111');
Add color pickers (place them near the subject/message fields):
<div className="flex gap-6 mb-4">
    <div>
        <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1.5">
            {t('admin.backgroundColor') || 'Background'}
        </label>
        <div className="flex items-center gap-2">
            <input
                type="color"
                value={bgColor}
                onChange={(e) => setBgColor(e.target.value)}
                className="w-12 h-9 border border-gray-300 cursor-pointer"
            />
            <span className="text-xs text-gray-500">{bgColor}</span>
        </div>
    </div>
<div>
<label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1.5">
    {t('admin.textColor') || 'Text'}
</label>
        <div className="flex items-center gap-2">
                <input
                type="color"
                value={textColor}
                onChange={(e) => setTextColor(e.target.value)}
                className="w-12 h-9 border border-gray-300 cursor-pointer"
            />
            <span className="text-xs text-gray-500">{textColor}</span>
        </div>
    </div>
</div>

Add the live preview (the "review"). Place it below the form, before the send button:
<div className="mb-6">
    <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-2">
        {t('admin.preview') || 'Preview'}
    </label>
    <div className="border border-gray-200 p-2">
        <div style={{ backgroundColor: bgColor, padding: '32px', maxWidth: '560px', margin: '0 auto' }}>
            {imageUrl && (
                <img
                    src={getImageUrl(imageUrl)}
                    alt=""
                    style={{ width: '100%', height: 'auto', marginBottom: '20px', display: 'block' }}
                />
            )}
            {subject && (
                <h1 style={{ fontSize: '22px', fontWeight: 800, textTransform: 'uppercase', margin: '0 0 20px', color: textColor }}>
                    {subject}
                </h1>
            )}
            <div
                className="broadcast-content"
                style={{ fontSize: '14px', lineHeight: 1.6, color: textColor }}
                dangerouslySetInnerHTML={{ __html: message }}
            />
        </div>
    </div>
</div>

This mirrors the email layout exactly, so what the admin sees is what gets sent.
Update handleSend to include the colors:
const res = await sendBroadcast({
    subject,
    message,
    imageUrl,
    bgColor,
    textColor,
    channels: selectedChannels,
});

And reset them after sending:
jsxsetSubject('');
setMessage('');
setImageUrl('');
setBgColor('#ffffff');
setTextColor('#111111');
Update handleReuse to load colors from history too:
jsxconst handleReuse = (broadcast) => {
    setSubject(broadcast.subject || '');
    setMessage(broadcast.message || '');
    setImageUrl(broadcast.imageUrl || '');
    setBgColor(broadcast.bgColor || '#ffffff');
    setTextColor(broadcast.textColor || '#111111');
    setSelectedChannels(broadcast.channels.split(',').map(c => c.trim()));
    window.scrollTo({ top: 0, behavior: 'smooth' });
    toast.info(t('messages.loadedIntoForm'));
};

One thing to decide on the colors + signature footer interaction: you recently added the SIGNATURE_FOOTER that gets 
injected into every email via sendHtmlEmail. That footer has its own white background and dark text. If an admin picks a 
dark background for a broadcast, the footer (injected after the colored content) will still be white/dark — which might 
look like a mismatched block at the bottom.
Two options: either let the footer keep its own neutral styling (a clean separator at the bottom regardless of broadcast 
colors — usually fine), or make the footer inherit the broadcast background. I'd suggest leaving the footer neutral — it 
reads as a consistent brand sign-off. But if you want the colored background to extend through the footer, tell me and 
I'll adjust how sendHtmlEmail wraps things.
