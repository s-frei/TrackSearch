<div align="center">
  <b><h1>TrackSearch</h1></b><br>
  <p>
  <a href="CONTRIBUTING.md">
  <img alt="Contributions welcome" src="https://img.shields.io/badge/contributions-welcome-brightgreen">
  </a>
  <a href="https://github.com/s-frei/TrackSearch/releases"> 
  <img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/s-frei/tracksearch">
  </a>
  </p>
</div>

## What is it ?

TrackSearch is for searching track metadata on different sources, like Youtube and SoundCloud for now and to expose the
URL of the underlying audio stream in the highest resolution. That offers the possibility to hand them over to other
programs which are able to process them, like [VLC](https://www.videolan.org/vlc/), or Firefox which can display the 
audio directly for example.

**Note:** TrackSearch isn't using any API-Key, it uses the public Rest-API.

## Supported sources

Since TrackSearch focuses on just exposing the audio streams and to search for music (although YouTube offers more than 
music) I decided to add following providers first for now:

- YouTube
- SoundCloud

There could be more added if there is interesting content offered to go for.

#### Current features:

- Search for keywords
- Paging of results
- Expose audio stream url
- Interact with multiple clients asynchronous

## How to use it ?

#### Dependency

For now using jitpack. GitHub packages does not allow to download without authentication through token.
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.s-frei.TrackSearch</groupId>
    <artifactId>tracksearch</artifactId>
    <version>0.6.0</version>
</dependency>
```

#### Example usage

```java
//Client to search on all available sources asynchronous
MultiTrackSearchClient searchClient = new MultiSearchClient();

//Client for explicit source
TrackSearchClient<SoundCloudTrack> explicitClient = new SoundCloudClient();

//Do the searching
TrackList<Track> tracksForSearch = searchClient.getTracksForSearch("your keywords")

//Get the audio stream
List<Track> tracks = tracksForSearch.getTracks();
String streamUrl = tracks.get(any).getStreamUrl();

//Get next tracks
TrackList<Track> nextTracks = searchClient.getNext(tracksForSearch);
```

## Why is this done ?

I haven't found anything which is capable of doing this kind of stuff, except it offered something similar and could
be abused for this, or it wasn't written in Java.


## Develop

Run following command in the root directory.

#### Build

```shell script
mvnw clean install
```

For detailed tests activate the `detailed-client-test` maven profile.

#### Stuff to be added

Feel free to contribute!

- Standalone web module offering a RESTful API and Frontend
- More documentation
- Playlist URL search
- Direct audio stream URL resolving
