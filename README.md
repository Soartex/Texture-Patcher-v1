## Texture Pack Patcher

### _Original by REDX36, modified by Chefe_

See [soartex website](http://soartex.net/patcher) for more information.

* * *

I forked the original Patcher because I prefer a gradle based build and want to modify some stuff based on my personal preferences.

To use this project, just clone the repository and run (make sure you have at least a gradle wrapper installed)

```
gradle clean assemble
```

Using the Patcher requires you (temporary, see [this bug](https://github.com/Soartex-Fanver/Texture-Patcher/issues/57#issuecomment-46576314)) to host your own config.json, since the soartex ones won't load due a 403 on cloudfare's "human check".
Be sure to write correct URLs in the src/main/resources/externalconfig.txt before you compile. You can add a second line link to your own branches.json.
I recommend getting the soartex ones:

http://soartex.net/texture-patcher/data/config.json and http://soartex.net/texture-patcher/data/branches.json

Note that you need the get the config(X.X.X).json for each branch you want to have available in the Patcher and the link in the branches.json have to match you hosting location as well. If you want to have only certain ones, just delete the rest and be sure to have vaild json (notice of the ',').

Example externalconfig.txt

```
http://example.com/path/to/your/config.json
http://example.com/path/to/your/branches.json
```


__This project is compatible with Java 6 and up.__

* * *

This project is liscensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
