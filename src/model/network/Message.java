package model.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import application.ApplicationType;

public class Message extends Header {
	
	
	private Object data;
	
	private ApplicationType appType;
	private MessageType type;
	private Integer priority;
	
	private byte[] payload;
	
	

	public Message(long sourceId, long destId, Object data, MessageType type, ApplicationType appType) {
		this.setSourceId(sourceId);
		this.setDestId(destId);
		this.setData(data);
		this.setType(type);
		this.setAppType(appType);
	}

	
	/**
	 * 
	 * @param data - object containing effective data
	 */
	public void setData(Object data) {
		
		this.data = data;
	}
	
	/**
	 * 
	 * @return message data (payload)
	 */
	public Object getData() {
		
		return data;
	}
	
	/**
	 * 
	 * @param appType - application type of message
	 */
	public void setAppType(ApplicationType appType) {
		
		this.appType = appType;
	}
	
	/**
	 * 
	 * @return - applicationType of the message
	 */
	public ApplicationType getAppType() {
		
		return appType;
	}
	
	/**
	 * 
	 * @return the deserialized message object
	 */
	public Serializable getPayload() {
		
		Serializable msg = null;
		// check if payload is not null
		assert this.payload != null;
		
		ByteArrayInputStream bis = new ByteArrayInputStream(this.payload);
		ObjectInput in = null;
		
		try {
			in = new ObjectInputStream(bis);
			msg = (Serializable)in.readObject();
		}
		catch(IOException e) {
			
			Logger.getLogger(Message.class.getName())
				.log(Level.SEVERE, "Error in serializing object: " + e);
		}
		catch(ClassNotFoundException e) {
			
			Logger.getLogger(Message.class.getName())
				.log(Level.SEVERE, "Error in serializing object: " + e);
		}
		finally {
			try {
				bis.close();
			}
			catch (IOException e) {
				Logger.getLogger(Message.class.getName())
					.log(Level.SEVERE, "Error in serializing object: " + e);
			}
			try {
				if (in != null) {
					in.close();
				}
			}
			catch (IOException e) {
				Logger.getLogger(Message.class.getName())
					.log(Level.SEVERE, "Error in serializing object: " + e);
			}
		}
		
		return msg;
	}
	

	/**
	 * 
	 * @param type - message type
	 */
	public void setType(MessageType type) {
		
		this.type = type;
	}
	
	/**
	 * 
	 * @return - message type
	 */
	public MessageType getType() {
		
		return type;
	}
	
	/**
	 * 
	 * @return - priority - message priority, used in the messages queue
	 */
	public int getPriority() {
		
		return priority;
	}
	
	/**
	 * 
	 * @param priority - message priority, used in the messages queue
	 */
	public void setPriority(int priority) {
		
		this.priority = priority;
	}
	/**
	 * 
	 * @param s - Serialiazable object, the Message object which
	 * will be converted to byte[]
	 */
	public void setPayload(Serializable msg) {
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		
		try {
			out = new ObjectOutputStream(bos);   
			out.writeObject(msg);
			this.payload = bos.toByteArray();
		}
		catch(IOException e) {
			
			Logger.getLogger(Message.class.getName())
				.log(Level.SEVERE, "Error in serializing object: " + e);
		}
		finally {
			try {
				if (out != null) {
					out.close();
				}
			}
			catch (IOException e) {
				
				Logger.getLogger(Message.class.getName())
					.log(Level.SEVERE, "Error in serializing object: " + e);
			}
			try {
				bos.close();
			} catch (IOException e) {
				
				Logger.getLogger(Message.class.getName())
					.log(Level.SEVERE, "Error in serializing object: " + e);
			}
		}
		
		assert this.payload != null;
	}
	
	
}
