qtp-zip
============
  
ZBot Interceptor (ZIP) for QTP using cScript

[Getting Started](https://github.com/zeedeveloper/qtp-zip/wiki/Getting-Started "Read Getting Started on wiki page")

[API Docs](http://zeedeveloper.github.com/qtp-zip/apidocs/ "Access javadocs") 
- - -
*Here is how this ZIP works:*  
1. This ZIP uses cscript to launch QTP. A temporary CScript file is generated `(c:\automation\qtp\qtpRunner.vbs)` dynamically containing path to the QTP script (provided by Zephyr Server in payload).  
2. After script execution is completed, zip looks for zephyr.png in results folder. If one found, it will upload it to Zephyr Server, else latest result.xml is uploaded.  
`This logic can be modified to upload multiple files at once.`  
3. Parses latest xml result output and upload the status to Zephyr server. This can further be enhanced to upload results at each step. 

