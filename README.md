# Lunchbot
[![Build Status](https://api.travis-ci.org/mturlo/lunchbot.svg?branch=develop)](https://travis-ci.org/mturlo/lunchbot)

Lunchbot is a Slack bot to help organise lunch ordering in teams. When ordering in a group, it is often troublesome to keep track of who wants to participate, what food do they want and if they paid the ordering person (the Lunchmaster). This bot's goal is to help make that process more efficient and organised.

![alt tag](https://raw.githubusercontent.com/mturlo/lunchbot/develop/sample.png)

_([pmxbot](https://github.com/yougov/pmxbot) makes a brief appearance here)_

## Installation

Build the artifact using `sbt assembly`. In your `target` directory you'll find an executable file - run it to start the app.

You'll need to [set up a bot user](https://api.slack.com/bot-users) for the bot.
 
Then, set your bot API token at `SLACK_API_KEY` environment variable, so: `export SLACK_API_KEY=my_bot_key`.

## Usage

Bot reacts to mentions, so if you set it up at the @lunchbot handle, you direct commands to it. Bot assigns reactions to messages processed successfully or responds textually with errors. 

The available commands are: _(Copied output from the `help` command)_

usage: <@lunchbot> `[command]` `[args...]`
* `create` `<name or URL of the place>` - creates a new lunch at `<name or URL of the place>`
* `close` - closes current lunch for order changes
* `open` - opens current lunch for further order changes
* `finish` - finishes current lunch
* `summary` - returns lunch summary
* `poke` - pokes all eaters that are lazy with their order
* `kick` `<eater to kick>` - kicks `<eater to kick>` from the current lunch
* `join` - joins the current lunch
* `leave` - leaves the current lunch
* `choose` `<food name>` - chooses food with `<food name>`
* `pay` - notifies about payment being made
* `help` - prints this usage text
* `stats` - prints historic orders statistics

### Commands

The general state flow of a lunch is:
 
*create* -> _(eaters join)_ -> _(eaters choose food)_ -> *close* -> _(eaters pay)_ -> *finish*

The user which issues the `create` command becomes the Lunchmaster, which allows him to administer the lunch state. 

All other users `join` the lunch to indicate they're interested in participation, then `choose` the food they want. You can `choose` again to overwrite your previous choice or `leave` the lunch if you've changed your mind. 

As Lunchmaster, you can `poke` eaters to have the bot @mention them to hurry up with their choice. You can also `kick` eaters from the lunch instance. 

When a lunch is *closed* no more eaters can join and this indicates that the Lunchmaster grabs the phone / app / website of the restaurant and proceeds with placing the order for everyone.

When the order is *closed* the eaters can issue `pay` commands to notify the Lunchmaster about making the payment. At this state, `poke` works on eaters, who have not yet paid.

At any point, any user can issue `summary` to see the current lunch state in detail.

Finally, you conclude the process with a `finish`.

### Statistics

The `stats` command returns counts per user, of how many times have they been the Lunchmaster. This can potentially help you resolve quarrels on who should order the next time or just be there for fun. 

Statistics are stored in a persistent manner, along with all of the lunch state and state history, so bot crashes won't have you lose your precious lunch order.

## Configuration

You can customise your bot messages in `messages.conf`. 

Other (less interesting) params are kept in `application.conf`. 

The bot uses [Akka Persistence](http://doc.akka.io/docs/akka/current/scala/persistence.html) with [LevelDB](https://github.com/google/leveldb) journal plugin and local snapshot store - it will create `journal/` and `snapshots/` directories in its home directory for that.

## License

Copyright (c) 2016-2017 Maciej Turło

Published under The MIT License, see LICENSE
