# RhythmboxPOPM
Scripts for dealing with POPM ID3 frames and Rhythmbox.

As is well known, RB uses an XML database to store its music files, together with ratings and play counts. However, it does not read the ID3 POPM frame of a music file, if it exists, making it harder for people transferring rated music from other players to this one (for example when switching from Windows to Linux). These scripts are trying to make that easier.

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
java -cp /usr/local/bin/POPMToRhythmbox.jar POPMToRB ~/Documents/.rbtopopm_temp.txt
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
This takes the ratings and play counts from the Rhythmbox database and writes them to the files, deleting all previous ratings and play counts.

Usage: rbtopopm email@address [files]

# popmtorb
This takes the ratings and play counts from the music files and writes them to the Rhythmbox database, potentially overwriting existing ones.
Behaviour:
  - if the music file has no rating and no play count, it is skipped.
  - if the file has a rating and play count and if both are the same as in RB, the file is skipped.
  - if the file has a rating and play count and one/both are different from the RB one
    - if no flag option is passed, the user is prompted: they are shown the conflicting values and asked if the database should be overwritten (not recommended for huge libraries of music).
    - if the -f flag option is used this prompt will not appear, the file will be skipped.
    - if the -o flag option is used this prompt will not appear, the file will be updated.

Usage: popmtorb [-f|-o] [files] or popmtorb [files] [-f|-o] (the -f and -o flags are optional).
