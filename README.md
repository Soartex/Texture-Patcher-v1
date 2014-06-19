## Texture Pack Patcher

### _Original by REDX36, modified by Chefe_

See [soartex website](http://soartex.net/patcher) for more information.

* * *

To use this project, just clone the repository and run (make sure you have at least a gradle wrapper installed)

```
gradle clean assemble
```

Using the Patcher requires you to host you own config.json, since the soartex ones wont load on cloudfare due a 403 on their "human check".
Be sure to get correct URLs in the src/main/resources/externalconfig.txt. You can add a second line link to your own branches.json.

Example externalconfig.txt

```
http://example.com/path/to/your/config.json
http://example.com/path/to/your/branches.json
```


__This project is compatible with Java 6 and up.__

* * *

This project is liscensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
