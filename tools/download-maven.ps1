$MAVEN_VERSION="3.2.3";
$MAVEN_URL="http://archive.apache.org/dist/maven/maven-3/3.2.3/binaries/apache-maven-3.2.3-bin.zip";


function Expand-ZIPFile($file, $destination){
    $shell = new-object -com shell.application
    $zip = $shell.NameSpace($file)
    foreach($item in $zip.items()){
        $shell.Namespace($destination).copyhere($item, 16)
    }
}

function Download-Maven($destination){
    $webclient = New-Object System.Net.WebClient;

    Write-Output $destination;
    Write-Output $MAVEN_URL;
    if (-Not (Test-Path $target)){
        $webclient.DownloadFile($MAVEN_URL,$target);
    }
}

function Check-Maven-Version($mavenPath){
    if (-Not (Test-Path $mavenPath)){
        return;
    }
    [Environment]::SetEnvironmentVariable("M2_HOME",$null); #in case there is M2_HOME set we will get wrong results when running mvn -version

    $result = iex "$mavenPath\bin\mvn.bat -version" | Select-String ("Apache Maven " + $MAVEN_VERSION);    
    return ,-Not([string]::IsNullOrEmpty($result));


}

function Get-ScriptDirectory{
  $Invocation = (Get-Variable MyInvocation -Scope 1).Value
  Split-Path $Invocation.MyCommand.Path
}

$toolsPath = Get-ScriptDirectory;
$mavenPath = "$toolsPath\maven";
$target = "$toolsPath\maven.zip";
$rightMavenVersion = Check-Maven-Version -mavenPath $mavenPath;


if (-Not ($rightMavenVersion) -and (Test-Path $mavenPath)){
    Write-Output "removing current outdated maven";
    Remove-Item $mavenPath -recurse;
}

if (-Not(Test-Path $mavenPath)){
    if (-Not (Test-Path $target)){
        Download-Maven -destination $target;    
    }    
    Expand-ZIPFile -file $target -destination $toolsPath;
    Move-Item "$toolsPath\apache*" "$toolsPath\maven"
    Remove-Item $target;
}

