# Background removal flag

`REMOVEBG_ENABLED=false` (default) disables remove.bg calls for both local storage
and Cloudinary. Uploads requesting `removeBackground=true` then store the original
image. Normal uploads are unchanged.

To enable, set `REMOVEBG_ENABLED=true` and configure `app.removebg.api-key` using the
appropriate profile's existing secret source. Restart the backend after changing
the flag. This is a startup configuration flag, not a live toggle.

The local profile inherits the flag from `application.yml`. Production also
inherits it; to enable there, pass the variable into the backend container through
the existing deployment environment. No production deployment was performed.

The WebClient response limit is now 20 MiB instead of the default 256 KiB, allowing
larger processed PNG images. This addresses a possible failure cause; a successful
real remove.bg response has not yet been verified.

Admin forms fetch `GET /api/upload/features` (authenticated ADMIN/EMPLOYEE access)
and show background-removal checkboxes only when `removeBackgroundEnabled` is true.
The option stays hidden during loading or if fetching fails. Refresh the frontend
after restarting the backend to read a changed flag.
