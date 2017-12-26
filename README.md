# RhythmboxPOPM
Scripts for dealing with POPM ID3 frames and Rhythmbox.

As is well known, RB uses XML database to store its music files, together with rating. However, it does not read the ID3 rating frame of a music file, if it exists, making it harder for people transferring rated music from other player to this one (for example when switching from Windows to Linux). These scripts are trying to make that easier.

Note: the popmtorb is very fast: with -f option it goes through 2000 files in less than 5 seconds. However the rbtopopm is much slower, taking about 5 minutes for all 2000. I understand the problem and I am trying to make it faster. Any input on this is appreciated.

Another great thing about these tools is that you can use wildcards and thus select even whole libraries with one line:
```bash
shopt -s globstar
rbtopopm ~/Music/**/*.mp3
  ```

This will select all mp3 files in the Music folder, no matter their place in the file tree. However your shell must support glostar; for other shells there are other solutions online.


For this to work I am using two dependencies - tools called eyeD3 and exiftool, both great tools, worth cheking out if you want to develop something yourself ;)

# Installation
Download and run build. It will install dependencies, compile files and move them to appropriate places. It needs root access.

# rbtopopm
This takes the rating from the Rhythmbox database and writes it to the file, deleting all previous ratings.
It writes it in both Rhythmbox (1-5) and Windows Media Player (1-255) format.

Usage: rbtopopm [files]
   
# popmtorb
This takes the rating from the music file and writes it to the Rhythmbox database, potentially overwriting an existing one.
Behaviour:
  - if the music file has no rating, it is skipped.
  - if the file has rating and is the same as the one in RB, the file is skipped.
  - if the file has rating and it is differnt from the RB one, the user is promted: he is shown both ratings and asked if the database should be overriten. 
    - if the -f option is used this prompt will not appear, the file will be skipped (this is recommended for huge libraries of music)

Usage: popmtorb [-f] [files] or popmtorb [files] [-f] (the -f is optional).
