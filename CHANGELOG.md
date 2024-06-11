Changelog
=========

0.11.0 - unreleased
-------------------

Nothing changed yet...

0.10.1
------

**Features:**

- Updated dependencies

**Bugfixes:**

- Fix YouTube tracks parsing through parse missing info on demand

0.10.0
------

**Breaking:**

- Stream URLs are now wrapped in `TrackStream` to also have related data like `TrackFormat` available
  and can be now accessed with `TrackStream.url()`
    - `Track.getStreamURL()` changed to `Track.getStream()`
    - `TrackSearchClient.getStreamURL()` changed to `TrackSearchClient.getTrackStream()`

**Features:**

- Determine the best audio format for SoundCloudTrack
- Various code and performance improvements
- Updated dependencies

**Bugfixes:**

- Fix YouTube tracks parsing through avoiding sponsored

0.9.0
-----

**Breaking:**

- Bump to JDK 17

**Features:**

- Get tracks by URL
- Improved tests
- Updated dependencies

**Bugfix:**

- Fix YouTube tracks parsing

0.8.2
-----

**Features:**

- Updated dependencies
- First shot of getting YouTubeTrack by URL

**Bugfix:**

- Fix YouTube tracks parsing

0.8.1
-----

**Features:**

- Updated dependencies

0.8.0
-----

**Breaking:**

- The *length* (`java.lang.Long`) field of Track is now *duration* (`java.time.Duration`)
- The TrackList is now extending `List` and `getTracks()` isn't possible anymore

**Features:**

- Obtain next/paged tracks using the TrackList `next()` function
- Improved code quality and readability especially for JSON processing
- Improved tests
- Improved duration parsing
- Updated dependencies

**Bugfixes:**

- SC: When a track has no thumbnail default to channel thumbnail

0.7.3
-----

**Features:**

- Additional path for YouTube paging information
- Improved tests and logging

**Bugfixes:**

- Avoid parsing advertisements on YouTube search results

0.7.2
-----

**Feature:**

- Additional youtube streaming data path 
- Additional youtube response data path

0.7.1
-----

**Feature:**

- Additional youtube video data path
- Increase default URL resolving retries
- Avoid resolving of streams and ads
- Updated dependencies

**Bugfix:**
  
- Fix youtube track length resolving

0.7.0
-----

**Feature:**

- Improve request and function caching for better performance
- Improve log output
- Avoid resolving of not ready youtube streams
- Updated dependencies

**Bugfix:**

- Fix stream URL resolving through retry 'once' on failure. This is not
  improving performance but fixes the issue when resolving fails for unknown
  circumstances.

0.6.4
-----

**Bugfix:**

- Fix soundcloud stream resolving (.m3u8 fallback)
- Fix YouTube signature decryption

0.6.3
-----

**Feature:**

- Additional metadata resolving trough new object on the track layer
- Maven caching to test runner

0.6.2
-----

**Feature:**

- Fallback for youtube player script
- Daily test runner to verify functionality

0.6.1
-----

**Major:**
- Switch to Java 11

**Feature:**
- Added caching and cookie storing
- Multiple code enhancements

**Bugfix:**
- Fix youtube stream resolving

0.6.0
-----

**Feature:**
- Unit tests for all clients
- Better code readability and enhanced JSON processing

**Bugfix:**
- Additional YouTube JSON path to avoid null values

0.5.6
-----

**Feature:**
- Alternative youtube track format route
- Fallback to video stream when no applicable audio
  stream could be resolved for youtube track

**Bugfix:**
- Cleaner title replacements

0.5.5
-----

**Feature:**
- Added direct stream url exposing using the track object
- Added some documentation to the track interface

**Bugfix:**
- Added another optional paging path

0.5.4
-----

**Bugfix:**
- Added optional path to YouTube tracks content
- Fixed YouTube cipher field and added optional one

0.5.3
-----

**Build:**
- Added license
- Added repository

0.5.2
-----

**Feature:**
- Added some documentation to main interfaces
- Build sources and documentation
- Deploy GitHub packages

**Bugfix:**
- Removed redundant logback configuration

0.5.1
-----

**Bugfix:**
- Fixed YouTube URL resolving bug

0.5.0
-----

**Feature:**
- Basic library added capable of searching on YouTube and SoundCloud
- Two different clients (YT & SC)
- Multi client to search asynchronous on clients
- Paging of search results on all clients 
