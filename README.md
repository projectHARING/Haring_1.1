# H.A.R.I.N.G
HARING was commissioned by iii [instrumentinventors.org](http://iiinitiative.org) as part of the project [audio-DH](http://audiodh.nl/?password-protected=login&redirect_to=http%3A%2F%2Faudiodh.nl%2F), a project directed by Francisco Lopez.

HARING was designed by Francisco Lopez together with Andrea Vogrig and Darien Brito and programmed in SuperCollider by Andrea Vogrig and Darien Brito. 

### Version 1.1
 - Doubled windows size FFT filter
 - Load tracks from path fix
 - Load EQ presets from file
 - default preset changed


### Download
##### OSX:
Compiled releases are available from the [github release page](https://github.com/projectHARING/Haring_1.1/releases). 

### Binary Installation
  - Download and extract the [latest pre-built release](https://github.com/projectHARING/Haring_1.0/releases)
  - Copy the file "H.A.R.I.N.G.app" to your Applications Folder
  - Run "H.A.R.I.N.G.app"
 
### Installation using SuperCollider 3.7.0
  - Download or clone this repository
  - Copy the contents of the folder named "HaringClasses" to your SuperCollider extensions folder.
     You can find out what folder that is by evaluating:
    
    ```
    Platform.userExtensionDir
    ```
    
    from within SuperCollider. Alternatively, you may install the extensions system-wide by copying to
    
    ```
    Platform.systemExtensionDir
    ```
    
  - Copy the content of "HaringApp" folder to your documents directory.
  - Open SuperCollider and execute the File "Haring.scd" 
