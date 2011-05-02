package javatools.administrative;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.database.Database;
import javatools.database.MySQLDatabase;
import javatools.database.OracleDatabase;
import javatools.database.PostgresDatabase;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;



/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).
  

  
 

Provides an interface for an ini-File. The ini-File may contain parameters of the form
<PRE>
parameterName = value
...
</PRE>
It may also contain comments or section headers (i.e. anything that does not match the
above pattern). Parameter names are not case sensitive. Initial and terminal spaces
are trimmed for both parameter names and values. Boolean parameters accept multiple
ways of expressing "true" (namely "on", "true", "yes" and "active").<P>

To avoid passing around object handles, this class does not function as an object!
There is only one "static object". Example:
<PRE>
  // Read data from my.ini
  this.init("my.ini");
  // Abort with error message if the following parameters are not specified
  this.ensureParameters(
     "firstPar - some help text for the first parameter",
     "secondPar - some help text for the secondparameter"
  );
  // Retrieve the value of a parameter
  String p=this.get("firstPar");
</PRE>
You can load parameters from multiple files. These will overlay.
You can use this also to reference a .ini file in another .ini file using the 'include' statement.
*/
public class NonsharedParameters {
  /** Thrown for an undefined Parameter */
  public static class UndefinedParameterException extends RuntimeException {    
    
    private static final long serialVersionUID = -7648653481162390257L;
    
    public UndefinedParameterException(String s, File f) {
      super("The parameter "+s+" is undefined in "+f);
    }
  }
   
  /** Holds the filename of the ini-file */
  public File iniFile=null;
  
  /** Holds the path that should be assumed to be the local path for all path values starting with ./ */ 
  public String localPath=null;
  
  /** Contains the values for the parameters*/
  public Map<String,String> values=null;

  /** Holds the pattern used for ini-file-entries */
  public Pattern INIPATTERN=Pattern.compile(" *(\\w+) *= *(.*) *");

  /** Holds words that count as "no" for boolean parameters */
  public FinalSet<String> no=new FinalSet<String>(new String [] {
        "inactive",
        "off",
        "false",
        "no",
        "none"
  });
  
  /** Constructors*/
  public NonsharedParameters(){};
  public NonsharedParameters(File iniFile)throws IOException{    
	  init(iniFile);
  };
  public NonsharedParameters(String iniFile)throws IOException{
	  init(iniFile);
  };
  
  public NonsharedParameters(File iniFile, String localPath)throws IOException{    
    if(localPath!=null)
    	this.localPath=localPath.endsWith("/")?localPath:localPath+"/";    		
    init(iniFile);
  };
  public NonsharedParameters(String iniFile, String localPath)throws IOException{
	if(localPath!=null)
	   	this.localPath=localPath.endsWith("/")?localPath:localPath+"/";
    init(iniFile);
  };  


  /** Returns a value for a file or folder parameter; same as getFile but returns the path as String */
  public String getPath(String s) throws UndefinedParameterException {
    if(localPath==null)
      return get(s);
    else{
      String path=get(s);
      if(path.startsWith("./"))
        return localPath+path.substring(3);
      else if(path.startsWith("../"))
          return localPath+path;
      else 
        return path;
    }
  }

  /** Returns a value for a file or folder parameter, returning the default value if undefined; same as getFile but returns the path as String*/
  public String getPath(String s, String defaultValue) throws UndefinedParameterException {
    return(isDefined(s)?getPath(s):defaultValue);
  }
  
  
  /** Returns a value for a file or folder parameter; same as getPath but returns an actual File instance */
  public File getFile(String s) throws UndefinedParameterException {
    return(new File(getPath(s)));
  }

  /** Returns a value for a file or folder parameter, returning the default value if undefined; same as getPath but returns an actual File instance */
  public File getFile(String s, File defaultValue) throws UndefinedParameterException {
    return(isDefined(s)?new File(getPath(s)):defaultValue);
  }

  /** Returns a value for an integer parameter*/
  public int getInt(String s) throws UndefinedParameterException {
    return(Integer.parseInt(get(s)));
  }

  /** Returns a value for an integer parameter returning the default value if undefined*/
  public int getInt(String s, int defaultValue) throws UndefinedParameterException {
    return(isDefined(s)?Integer.parseInt(get(s)):defaultValue);
  }

  /** Returns a value for an integer parameter*/
  public float getFloat(String s) throws UndefinedParameterException {
    return(Float.parseFloat(get(s)));
  }

  /** Returns a value for an integer parameter returning the default value if undefined*/
  public float getFloat(String s, float defaultValue) throws UndefinedParameterException {
    return(isDefined(s)?Float.parseFloat(get(s)):defaultValue);
  }
  
  /** Returns a value for an integer parameter*/
  public double getDouble(String s) throws UndefinedParameterException {
    return(Double.parseDouble(get(s)));
  }

  /** Returns a value for an integer parameter returning the default value if undefined*/
  public double getDouble(String s, double defaultValue) throws UndefinedParameterException {
    return(isDefined(s)?Double.parseDouble(get(s)):defaultValue);
  }

  /** Returns a value for a boolean parameter */
  public boolean getBoolean(String s) throws UndefinedParameterException  {
    String v=get(s);
    return(!no.contains(v.toLowerCase()));
  }

  /** Returns a value for a boolean parameter, returning a default value by default */
  public boolean getBoolean(String s, boolean defaultValue) {
    String v=get(s,defaultValue?"yes":"no");
    return(!no.contains(v.toLowerCase()));
  }

  /** Returns a value for a list parameter */
  public List<String> getList(String s) throws UndefinedParameterException  {
    if(!isDefined(s)) return(null);
    return(Arrays.asList(get(s).split("\\s*,\\s*")));
  }


 
  /** Returns a value for a parameter*/
  public String get(String s) throws UndefinedParameterException  {
    if(values==null) throw new RuntimeException("Call init() before get()!");
    String pname=s.indexOf(' ')==-1?s:s.substring(0,s.indexOf(' '));
    String v=values.get(pname.toLowerCase());
    if(v==null) throw new UndefinedParameterException(s,iniFile);
    return(v);
  }

  /** Returns a value for a parameter, returning a default value by default */
  public String get(String s, String defaultValue)  {
    if(values==null) throw new RuntimeException("Call init() before get()!");
    String pname=s.indexOf(' ')==-1?s:s.substring(0,s.indexOf(' '));
    String v=values.get(pname.toLowerCase());
    if(v==null) return(defaultValue);
    return(v);
  }
  
  /** Returns a value for a parameter. If not present, asks the user for it */
  public String getOrRequest(String s, String description) {
    if (values == null)
      throw new RuntimeException("Call init() before get()!");
    String v = values.get(s.toLowerCase());
    if (v == null) {
      D.println(description);
      v = D.read();
    }
    return (v);
  }

  /**
   * Returns a value for a parameter. If not present, asks the user for it and
   * adds it
   */
  public String getOrRequestAndAdd(String s, String description)
      throws IOException {
    String v = getOrRequest(s, description);
    add(s, v);
    return (v);
  }

  /**
   * Returns a value for a parameter. If not present, asks the user for it and
   * adds it
   */
  public boolean getOrRequestAndAddBoolean(String s, String description)
      throws IOException {
    boolean v = getOrRequestBoolean(s, description);
    add(s, v ? "yes" : "no");
    return (v);
  }

  /** Returns a value for a parameter. If not present, asks the user for it */
  public File getOrRequestFileParameter(String s, String description) {
    while (true) {
      String fn = getOrRequest(s, description);
      File f = new File(fn);
      if (f.exists())
        return (f);
      D.println("File not found",fn);
      remove(s);
    }
  }
  
  /** Returns a value for a parameter. If not present, asks the user for it */
  public boolean getOrRequestBoolean(String s, String description) {
    while (true) {
      String fn = getOrRequest(s, description).toLowerCase();
      if (fn.equals("true") || fn.equals("yes"))
        return (true);
      if (fn.equals("false") || fn.equals("no"))
        return (false);
    }
  }

  /** Returns a value for a parameter. If not present, asks the user for it */
  public int getOrRequestInteger(String s, String description) {
    while (true) {
      String fn = getOrRequest(s, description);
      try {
        return(Integer.parseInt(fn));
      } catch(Exception e) {}
      remove(s);
    }
  }
  
  /** Adds a value to the map and to the ini file, if not yet there */
  public void add(String key, String value) throws IOException {
    if (values == null || iniFile == null)
      throw new RuntimeException("Call init() before get()!");
    if (values.containsKey(key.toLowerCase()))
      return;
    values.put(key.toLowerCase(), value);
    Writer w = new FileWriter(iniFile, true);
    w.write(key + " = " + value + "\n");
    w.close();
  }
  
  /** Removes a value from the mapping (NOT: from the file) */
  public String remove(String parameter) {
    return(values.remove(parameter.toLowerCase()));
  }

  /** sets a value for a parameter */
  public void set(String param, String value){
	  if(values==null) throw new RuntimeException("Call init() before get()!");
	  values.put(param, value);
  }
  
  /** Initializes the parameters from a file*/
  public void init(File f) throws IOException{
    init(f,true);
  }
  public void init(File f, boolean mainIni) throws IOException {
    if(f.equals(iniFile)) return;    
    if(mainIni){
      values=new TreeMap<String,String>();    
      iniFile=f;
    }
    if (!iniFile.exists()) {
      Announce.error("The initialisation file",
          iniFile.getCanonicalPath(),
          "was not found.");
    }
    for(String l : new FileLines(f)) {
      Matcher m=INIPATTERN.matcher(l);
      if(!m.matches()) continue;
      String s=m.group(2).trim();
      if(s.startsWith("\"")) s=s.substring(1);
      if(s.endsWith("\"")) s=s.substring(0,s.length()-1);      
      if(m.group(1).toLowerCase().equals("include")){
        if(s.startsWith("/"))
          init(s,false);
        else
          init(iniFile.getParent()+"/"+s,false);
      }
      else
        values.put(m.group(1).toLowerCase(),s);
        
    }
  }
  
  /** Seeks the file in all given folders*/  
  public void init(String filename, File... folders) throws IOException {
    boolean found=false;
    for(File folder : folders) {
      if(new File(folder, filename).exists()) {
        if(found) 
          throw new IOException("INI-file "+filename+"occurs twice in given folders");
        init(new File(folder, filename));
        found = true;
      }
    }
  }

  
  /** Initializes the parameters from a file*/
  public void init(String filename)throws IOException{
    init(filename,true);
  }
  public void init(String file, boolean mainIni) throws IOException {
    init(new File(file),mainIni);
  }
  
  
  /** Tells whether a parameter is defined */
  public boolean isDefined(String s) {
    if (values == null)
      throw new RuntimeException("Call init() before get()!");
    String pname = s.indexOf(' ') == -1 ? s : s
        .substring(0, s.indexOf(' '));
    return (values.containsKey(pname.toLowerCase()));
  }
  
  /** Reports an error message and aborts if the parameters are undefined.
   * p may contain strings of the form "parametername explanation"*/
  public void ensureParameters(String... p) {
    if (values == null)
      throw new RuntimeException("Call init() before ensureParameters()!");
    boolean OK = true;
    StringBuilder b = new StringBuilder(
        "The following parameters are undefined in ").append(iniFile);
    for (String s : p) {
      if (!isDefined(s)) {
        b.append("\n       ").append(s);
        OK = false;
      }
    }
    if (OK)
      return;
    Announce.error(b.toString());
  }

  /** Parses the arguments of the main method and tells whether a parameter is on or off */
  public boolean getBooleanArgument(String[] args,String... argnames) {
    String arg = " ";
    for (String s : args)
      arg += s + ' ';
    String p = "\\W(";
    for (String s : argnames)
      p += s + '|';
    if (p.endsWith("|"))
      p = p.substring(0, p.length() - 1);
    p += ")\\W";
    Matcher m = Pattern.compile(p).matcher(arg);
    if (!m.find())
      return (false);
    String next = arg.substring(m.end()).toLowerCase();
    if (next.indexOf(' ') != -1)
      next = next.substring(0, next.indexOf(' '));
    if (next.equals("off"))
      return (false);
    if (next.equals("0"))
      return (false);
    if (next.equals("false"))
      return (false);
    String previous = arg.substring(0, m.start()).toLowerCase();
    if (previous.indexOf(' ') != -1)
      previous = previous.substring(previous.lastIndexOf(' ') + 1);
    if (previous.equals("no"))
      return (false);
    return (true);
  }
  
  /** Deletes all current values*/
  public void reset() {
    iniFile=null;
    values=null;
  }
  
  /** Returns the database defined in this ini-file*/
  public Database getDatabase() throws Exception {
    ensureParameters("databaseSystem - either Oracle, Postgres or MySQL",
        "databaseUser - the user name for the database (also: databaseDatabase, databaseInst,databasePort,databaseHost,databaseSchema)",
        "databasePassword - the password for the database"
    );
        
    // Retrieve the obligatory parameters
    String system=this.get("databaseSystem").toUpperCase();
    String user=this.get("databaseUser");    
    String password=this.get("databasePassword");
    String host=null;
    String schema=null;
    String inst=null;
    String port=null;
    String database=null;

    
    // Retrieve the optional parameters
    try {
      host=this.get("databaseHost");
    } catch(Exception e){Announce.debug("Warning: "+e);};
    try {
      schema=this.get("databaseSchema");
    } catch(Exception e){Announce.debug("Warning: "+e);};
    try {
      port=this.get("databasePort");    
    } catch(Exception e){Announce.debug("Warning: "+e);};          
    try {
      inst=this.get("databaseSID");
    } catch(UndefinedParameterException e) {Announce.debug("Warning: "+e);}
    try {
      database=this.get("databaseDatabase");
    } catch(UndefinedParameterException e) {Announce.debug("Warning: "+e);}    
    
    // Initialize the database
    // ------ ORACLE ----------
    if(system.equals("ORACLE")) {
      return(new OracleDatabase(user,password,host,port,inst));
    }
    //  ------ MySQL----------
    if(system.equals("MYSQL")) {
      return(new MySQLDatabase(user,password,database,host,port));
    }
    //  ------ Postgres----------
    if(system.equals("POSTGRES")) {
      return(new PostgresDatabase(user,password,database,host,port,schema));
    }
    throw new RuntimeException("Unsupported database system "+system);        
  }
  
  /** Returns all defined parameters*/
  public Set<String> parameters() {
    return(values.keySet());
  }
  
  /** Test routine */
  public void main(String argv[]) throws Exception {
    System.err.println("Enter the name of an ini-file: ");
    init(D.r());
    D.p(values);
  }
}