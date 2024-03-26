<!--suppress HtmlDeprecatedAttribute -->
<div align="center">
  <b><h1>TrackSearch</h1></b><br>
  <p>
  <a href="CONTRIBUTING.md">
    <img alt="Contributions welcome" src="https://img.shields.io/badge/contributions-welcome-brightgreen">
  </a>
  <a href="LICENSE">
    <img alt="License" src="https://img.shields.io/github/license/s-frei/TrackSearch">
  </a>
  <a href="https://github.com/s-frei/TrackSearch/releases"> 
    <img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/s-frei/tracksearch">
  </a>
  <a href="https://mvnrepository.com/artifact/io.sfrei/tracksearch"> 
    <img alt="Maven Central release" src="https://img.shields.io/maven-central/v/io.sfrei/tracksearch">
  </a>
  <a href="https://github.com/s-frei/TrackSearch/actions/workflows/functionality-check.yml"> 
    <img alt="Functionality Test State" src="https://github.com/s-frei/TrackSearch/actions/workflows/functionality-check.yml/badge.svg">
  </a>
	<a href="https://github.com/s-frei/TrackSearch/actions/workflows/maven-test.yml"> 
    <img alt="Latest" src="https://github.com/s-frei/TrackSearch/actions/workflows/maven-test.yml/badge.svg">
  </a>
  </p>
</div>

## What is it ?

TrackSearch is for searching track metadata on different sources, like Youtube and SoundCloud for now and to expose the
URL of the underlying audio stream in the highest resolution. That offers the possibility to hand them over to other
programs which are able to process them, like [VLC](https://www.videolan.org/vlc/), or Firefox which can display the
audio directly for example.

**Note:** TrackSearch isn't using any API-Key, it uses the public API (like your browser).

## Supported sources

Since TrackSearch focuses on just exposing the audio streams and to search for music (although YouTube offers more than 
music) I decided to add following providers first for now:

- YouTube
- SoundCloud

There could be more added if there is interesting content offered to go for.

#### Current features :mag_right:

- Search for keywords
- Paging of track lists
- Expose audio stream url
- Interact with multiple clients asynchronous
- Get track metadata like: duration, channel, views, thumbnail, ...

## How to use it ? :books:

### Dependency

Maven dependency available on [Maven Central](https://search.maven.org/artifact/io.sfrei/tracksearch):

```xml
<dependency>
    <groupId>io.sfrei</groupId>
    <artifactId>tracksearch</artifactId>
    <version>0.8.2</version>
</dependency>
```

or from [GitHub Packages](https://github.com/s-frei/TrackSearch/packages) or directly from 
[GitHub Releases](https://github.com/s-frei/TrackSearch/releases).

### Getting started

For more information check the related interface documentation.

```java
// Client to search on all available sources asynchronous
MultiTrackSearchClient searchClient = new MultiSearchClient();

// Client for explicit source
TrackSearchClient<SoundCloudTrack> explicitClient = new SoundCloudClient();

// Search for tracks
TrackList<Track> tracksForSearch = searchClient.getTracksForSearch("your keywords")

// Get the audio stream
String streamUrl = tracksForSearch.get(anyPos).getStreamUrl();

// Get next tracks page
TrackList<Track> nextTracks = tracksForSearch.next();
```

## Why is this done ?

I haven't found anything which is capable of doing this kind of stuff, except it offered something similar and could be
abused for this, or it wasn't written in Java.

## Develop :hammer:

**Note:** **JDK 17** is required! (`sdk env install`)

Fire up following in your shell:

#### Build

```sh
$ ./mvnw clean install
```

#### Test

The *simple* [test runs daily](https://github.com/s-frei/TrackSearch/actions) to get notified when something is not
working. Test it on your own:

```sh
$ ./mvnw test
```

For detailed test (about 120 tracks for each client):

```sh
$ ./mvnw test -P detailed-client-test
```

---

## Contributing :handshake:

Feel free to contribute! - [How?](https://github.com/s-frei/TrackSearch/blob/develop/CONTRIBUTING.md)

#### Stuff to be added 

- Direct audio stream URL resolving
- Playlist URL search
