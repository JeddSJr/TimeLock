/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swingTimelock;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 *
 * @author Jedd
 */
public class timedLock implements Serializable{
      private static final long serialVersionUID = 532110321;
      private String id,path;
      private File file;
      private LocalDateTime end;
      
      public timedLock(String id,String path,File file,LocalDateTime end){
        this.id=id;
        this.path=path;
        this.file=file;
        this.end=end;
      }
      private void setId(String i){
          id = i;
      }
      private void setFile(File f){
          file = f;
      }
      private void setEnd(LocalDateTime e){
          end = e;
      }
      
      public String getId(){
          return id;
      }
      public String getPath(){
          return path;
      }
      public File getFile(){
          return file;
      }
      public LocalDateTime getEnd(){
          return end;
      }
      
      public String toString(){
          return("Lock on: "+getFile().getName()+" Ending: "+getEnd());
      }
  }