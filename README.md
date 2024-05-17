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
  <br>
  <a href="https://github.com/s-frei/TrackSearch/actions/workflows/functionality-check.yml"> 
    <img alt="Functionality Test State" src="https://github.com/s-frei/TrackSearch/actions/workflows/functionality-check.yml/badge.svg">
  </a>
	<a href="https://github.com/s-frei/TrackSearch/actions/workflows/maven-test.yml"> 
    <img alt="Latest" src="https://github.com/s-frei/TrackSearch/actions/workflows/maven-test.yml/badge.svg">
  </a>
  </p>
</div>

## What is it ?

*TrackSearch* is for searching track metadata on different sources, like *YouTube* and *SoundCloud* for now and to 
expose the URL of the underlying (audio) stream with the best quality. That offers the possibility to hand them over 
to other programs which are able to process them, like [VLC](https://www.videolan.org/vlc/), or *Firefox* which can 
display (most) streams directly, for example.

**Note:** TrackSearch isn't using any API-Key, it uses the public API (like your browser).

## Supported sources

Since TrackSearch focuses on just exposing the audio streams and to search for music (although YouTube offers more than 
music) I decided to add following providers for now:

![youtube](https://img.shields.io/badge/-YouTube-FF0000?style=plastic&logo=youtube&logoColor=white)
![soundcloud](https://img.shields.io/badge/-SoundCloud-FF3300?style=plastic&logo=soundcloud&logoColor=white)

There could be more added if there are interesting sources to go for.

**Note:** Those sources are accessed vie the public API without the use of any API key or similar.

#### Current features :mag_right:

- search
- paging
- (audio) stream url
- format
- multiple clients asynchronous
- metadata like: duration, channel, views, thumbnail, ...

## How to use it ? :books:

### Dependency

*TrackSearch* is available on [Maven Central](https://search.maven.org/artifact/io.sfrei/tracksearch):

```xml
<dependency>
    <groupId>io.sfrei</groupId>
    <artifactId>tracksearch</artifactId>
    <version>0.9.0</version>
</dependency>
```

```kotlin
implementation("io.sfrei:tracksearch:0.9.0")
```

on [GitHub Packages](https://github.com/s-frei/TrackSearch/packages) or directly from 
[GitHub Releases](https://github.com/s-frei/TrackSearch/releases/latest).

### Getting started

```java
// Client to search on all available sources asynchronous
MultiTrackSearchClient searchClient = new MultiSearchClient();

// Client for explicit source
TrackSearchClient<SoundCloudTrack> explicitClient = new SoundCloudClient();

try {
    // Search for tracks
    TrackList<Track> tracksForSearch = searchClient.getTracksForSearch("your keywords");
    TrackStream stream = tracksForSearch.get(0).getStream();

    // Stream URL with format
    final String streamUrl = stream.url();
    final TrackFormat format = stream.format();

    // Get next tracks page
    TrackList<Track> nextTracks = tracksForSearch.next();

    // Get a track for URL
    SoundCloudTrack trackForURL = explicitClient.getTrack("<soundcloud-url>");

} catch (TrackSearchException e) {
    // Damn
}
```

For more information check the related interface documentation or have a look into the 
[tests](https://github.com/s-frei/TrackSearch/blob/develop/src/test/java/io/sfrei/tracksearch/clients/ClientTest.java).

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

For detailed test (about ~250 tracks for each client):

```sh
$ ./mvnw test -P detailed-client-test
```

---

## Contributing :handshake:

Feel free to contribute! - [How?](https://github.com/s-frei/TrackSearch/blob/develop/CONTRIBUTING.md)
