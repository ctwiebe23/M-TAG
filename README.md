# M'TAG

A short program that takes a file directory as a command line argument and tags
every compatable audio file within the directory that has the naming convention
"Title - Artist.filetype" while ignoring all other files.

Uses ID3v1 for tagging ID3 compatable file types.
Currently compatable with .mp3 and .ogg files.

## Installation

Download from [Download] and run

    lein uberjar

from anywhere in the project to generate the standalone .jar file in
/target/uberjar.  This standalone .jar file is now completely independent of
the project and can be moved or run at will; in fact, this project can be
deleted afterword without impacting its performance.

## Usage

FIXME: explanation

    java -jar m-tag-1.0.0-standalone.jar [args]

## Examples

    $ java -jar m-tag-1.0.0-standalone.jar ../../resources/test_files -v
    =>

## License

Copyright © 2023

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
[Eclipse.org].

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at [GNU.org].

[Download]: http://example.com/FIXME
[Eclipse.org]: http://www.eclipse.org/legal/epl-2.0
[GNU.org]: https://www.gnu.org/software/classpath/license.html
