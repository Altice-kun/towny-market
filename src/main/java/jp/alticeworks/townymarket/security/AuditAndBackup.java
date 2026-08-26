package jp.alticeworks.townymarket.security;

import jp.alticeworks.townymarket.storage.Database;
import org.bukkit.plugin.java.JavaPlugin;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

public final class AuditAndBackup {
    private final JavaPlugin plugin; private final Database db;
    public AuditAndBackup(JavaPlugin plugin,Database db){this.plugin=plugin;this.db=db;}
    public void audit(String actor,String action,String target,String reason,String payload){try(var s=db.connection().prepareStatement("INSERT INTO audit_events(actor,action,target,reason,payload,created_at) VALUES(?,?,?,?,?,?)")){s.setString(1,actor);s.setString(2,action);s.setString(3,target);s.setString(4,reason);s.setString(5,payload);s.setLong(6,System.currentTimeMillis());s.executeUpdate();}catch(Exception e){plugin.getLogger().warning("Audit write failed: "+e.getMessage());}}
    public int purgeAudit30Days(){try(var s=db.connection().prepareStatement("DELETE FROM audit_events WHERE created_at<?")){s.setLong(1,System.currentTimeMillis()-30L*24*60*60*1000);return s.executeUpdate();}catch(Exception e){return 0;}}
    public Path backup(){try{Path dir=plugin.getDataFolder().toPath().resolve("backups");Files.createDirectories(dir);Path source=plugin.getDataFolder().toPath().resolve("market.db");byte[] data=Files.readAllBytes(source);byte[] encrypted=data;for(int i=0;i<5;i++)encrypted=crypt(encrypted,key(i),Cipher.ENCRYPT_MODE);String id=UUID.randomUUID().toString();Path out=dir.resolve(id+".alite");Files.write(out,encrypted,StandardOpenOption.CREATE_NEW);String sha=sha256(encrypted);try(var s=db.connection().prepareStatement("INSERT INTO backups(id,path,sha256,encrypted,created_at,status) VALUES(?,?,?,?,?,?)")){s.setString(1,id);s.setString(2,out.toString());s.setString(3,sha);s.setInt(4,1);s.setLong(5,System.currentTimeMillis());s.setString(6,"VALID");s.executeUpdate();}audit("SYSTEM","BACKUP_CREATE",id,"scheduled",sha);return out;}catch(Exception e){plugin.getLogger().warning("Backup failed: "+e.getMessage());return null;}}
    private byte[] crypt(byte[] input,byte[] key,int mode)throws Exception{byte[] iv=new byte[12];System.arraycopy(MessageDigest.getInstance("SHA-256").digest(key),0,iv,0,12);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(mode,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));return c.doFinal(input);}
    private byte[] key(int layer)throws Exception{String ip=InetAddress.getLocalHost().getHostAddress();byte[] b=("AltimeceEncryptAlgorizmLite:"+ip+":"+layer).getBytes(StandardCharsets.UTF_8);for(int i=0;i<10000;i++)b=MessageDigest.getInstance("SHA-256").digest(b);return b;}
    private String sha256(byte[] b)throws Exception{byte[] d=MessageDigest.getInstance("SHA-256").digest(b);StringBuilder s=new StringBuilder();for(byte v:d)s.append(String.format("%02x",v));return s.toString();}
}
