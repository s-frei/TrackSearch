Changelog
=========

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
