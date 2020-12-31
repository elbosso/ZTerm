# ZTerm
A terminal emulator

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


