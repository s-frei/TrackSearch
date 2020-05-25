# TrackSearch

## What is it ?

TrackSearch is to search for track metadata on different sources, like Youtube and SoundCloud for now and to expose the
URL of the underlying audio stream in the highest resolution. That offers the possibility to hand them over to other
programs which are able to process them, like [VLC](https://www.videolan.org/vlc/) for example.
TrackSearch isn't using any API-Key, it uses the public Rest-API.

## Supported sources

Since TrackSearch focuses on just exposing the audio streams and to search for music (I know YouTube offers more than 
music) I decided to add following providers first for now:

- YouTube
- SoundCloud

There could be more added if there is interesting content offered to go for.

## How to use it

```java
private String = "add content";
```

## Why is this done ?

I haven't found anything which is capable of doing this kind of stuff, except it offered something similar and could
be abused for this, or it wasn't written in Java.


## Develop

Run following commands in the root directory.

#### Build

```shell script
mvnw clean package
```