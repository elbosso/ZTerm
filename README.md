# ZTerm
A terminal emulator

<!---
[![start with why](https://img.shields.io/badge/start%20with-why%3F-brightgreen.svg?style=flat)](http://www.ted.com/talks/simon_sinek_how_great_leaders_inspire_action)
--->
[![GitHub release](https://img.shields.io/github/release/elbosso/ZTerm/all.svg?maxAge=1)](https://GitHub.com/elbosso/ZTerm/releases/)
[![GitHub tag](https://img.shields.io/github/tag/elbosso/ZTerm.svg)](https://GitHub.com/elbosso/ZTerm/tags/)
[![made-with-jasva](https://img.shields.io/badge/Made%20with-Java-9cf)](https://www.java.com)
[![GitHub license](https://img.shields.io/github/license/elbosso/ZTerm.svg)](https://github.com/elbosso/ZTerm/blob/master/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/elbosso/ZTerm.svg)](https://GitHub.com/elbosso/ZTerm/issues/)
[![GitHub issues-closed](https://img.shields.io/github/issues-closed/elbosso/ZTerm.svg)](https://GitHub.com/elbosso/ZTerm/issues?q=is%3Aissue+is%3Aclosed)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/elbosso/ZTerm/issues)
[![GitHub contributors](https://img.shields.io/github/contributors/elbosso/ZTerm.svg)](https://GitHub.com/elbosso/ZTerm/graphs/contributors/)
[![Github All Releases](https://img.shields.io/github/downloads/elbosso/ZTerm/total.svg)](https://github.com/elbosso/ZTerm)
[![Website elbosso.github.io](https://img.shields.io/website-up-down-green-red/https/elbosso.github.io.svg)](https://elbosso.github.io/)

## Introduction and the "Why?"

This is a project I found many years ago - it provides (kind of) a terminal emulation and primary
a learning opportunity to me. I recently found that the project had made its way onto github so
I forked it here.

A few years back I worked at a company where there was much serial communication going on - I
used this project then to build a "bells and whistles" version of a serial terminal. I also made 
several small fixes and enhancements as I went along. Sadly
that company is no longer in existence (and I did of course not take the stuff along that I made 
while getting my money from them). I therefore try at the moment whenever I find some spare minutes to 
remember, reimagine and reimplement those fixes and features. However, it is still very much a work 
in progress at this time.

I added an example app for illustration and testimg purposes. If you want it to do anything sensible or - dare I say it - useful,
you probably will have to nmmodify the source code.

## Testing

I test with (n)curses and use `dialog` for that mainly - the most profound test being `dialog --msgbox "huhu" 10 30`. 
Another thing I would like to use later on is 
https://invisible-island.net/vttest/#download but sadly at the moment it does not even start.

## State of the union

The project has at the moment altogether four emulation modes:

* ansi
* vt100
* xterm
* xterm-color

It is advisable to only use *vt100* or *xterm-color* in production at the moment as the other two fail even with the aforementioned 
`dialog`-test.

It is also advisable to explicitly set the `TERM` environment variable if you are going to start a shell (or
any other process for that matter) inside the emulator because else you get the terminal type `dumb` 
and that leads to all kinds of problems. And please try not to be funny by setting this environment variable
to anythong other then the choosen emulation (see VT100Tester for examples).

## Resources used

* https://github.com/jawi/jVT220
* https://man7.org/linux/man-pages/man7/charsets.7.html
* https://www.torsten-horn.de/techdocs/ascii.htm
* https://www.vt100.net/charsets/technical.html
* https://www.vt100.net/docs/la100-rm/chapter1.html
* https://www.xfree86.org/current/ctlseqs.html
* https://invisible-island.net/xterm/ctlseqs/ctlseqs.txt
* https://man7.org/linux/man-pages/man4/console_codes.4.html
* https://vt100.net/docs/vt510-rm/NEL.html
* https://vt100.net/docs/vt100-ug/chapter3.html

