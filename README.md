# RhythmboxPOPM
Scripts for dealing with POPM ID3 frames and Rhythmbox.

As is well known, RB uses an XML database to store its music files, together with ratings. However, it does not read the ID3 rating frame of a music file, if it exists, making it harder for people transferring rated music from other players to this one (for example when switching from Windows to Linux). These scripts are trying to make that easier.

Note: the popmtorb is very fast: with -f option it goes through 2000 files in less than 5 seconds. However the rbtopopm is much slower, taking about 5 minutes for all 2000. I understand the problem and I am trying to make it faster. Any input on this is appreciated.

Another great thing about these tools is that you can use wildcards and thus select even whole libraries with one line:
```bash
shopt -s globstar
rbtopopm ~/Music/**/*.mp3
  ```

<b> Note that the music folder must be referenced by whole path from the home folder (i.e. ~/Music/...) </b>

This will select all mp3 files in the Music folder, no matter their place in the file tree.

For very large numbers of files, or for shells not supporting globstar, you may want to instead use something like:
```bash
find ~/Music -name "*.mp3" > ~/Documents/.rbtopopm_temp.txt
java -cp /usr/local/bin/POPMToRhythmBox.jar POPMToRB ~/Documents/.rbtopopm_temp.txt
  ```


For this to work I am using two dependencies - tools called eyeD3 and exiftool, both great tools, worth checking out if you want to develop something yourself ;)

# Installation
Download and run build. It will install dependencies, compile files and move them to appropriate places. It needs root access. You need to have the Java JDK installed.

For example:
```bash
sudo apt-get install openjdk-11-jdk
bash build
  ```

# rbtopopm
This takes the rating from the Rhythmbox database and writes it to the file, deleting all previous ratings.
It writes it in both Rhythmbox (1-5) and Windows Media Player (1-255) format.

Usage: rbtopopm [files]
   
# popmtorb
This takes the rating from the music file and writes it to the Rhythmbox database, potentially overwriting an existing one.
Behaviour:
  - if the music file has no rating, it is skipped.
  - if the file has rating and is the same as the one in RB, the file is skipped.
  - if the file has rating and it is different from the RB one
    - if no flag option is passed, the user is prompted: he is shown both ratings and asked if the database should be overwritten (not recommended for huge libraries of music).
    - if the -f flag option is used this prompt will not appear, the file will be skipped.
    - if the -o flag option is used this prompt will not appear, the file will be overwritten.

Usage: popmtorb [-f|-o] [files] or popmtorb [files] [-f|-o] (the -f and -o flags are optional).
