# lunchbot
For all your lunch (automation) needs!

## Installation

Set your bot API token at `SLACK_API_KEY` env, so: `export SLACK_API_KEY=my_bot_key`.

## Usage

Bot reacts to mentions, so if you set it up at @lunchbot handle:

_(Copied output from the `help` command)_

usage: <@lunchbot> `[command]` `[args...]`
* `create` `<name or URL of the place>` - creates a new lunch at `<name or URL of the place>`
* `cancel` - cancels current lunch
* `summary` - returns lunch summary
* `poke` - pokes all eaters that are lazy with their order
* `kick` `<eater to kick>` - kicks `<eater to kick>` from the current lunch
* `join` - joins the current lunch
* `leave` - leaves the current lunch
* `choose` `<food name>` - chooses food with `<food name>`
* `pay` - notifies about payment being made
* `help` - prints this usage text