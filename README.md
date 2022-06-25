# Webhook Trigger
An event-based custom trigger that triggers off a simple HTTP GET or POST

## Features
 * Starts web server on bot runner machine that listens on defined port and path
 * Support GET and POST requests
 * Passes POST body to a record value to parse within the bot
    
### How do I use this package?
1. Enter port and path inputs to create the URI (e.g. http://<your_device_hostname>:<port>/<path>)
2. Run bot with triggers, or set up event trigger in Control Room
3. Send POST or GET request to the defined URI

