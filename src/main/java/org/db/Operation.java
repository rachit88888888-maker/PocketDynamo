package org.db;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Operation implements Serializable {

        private final Command command;
        private final String key;
        private final String value;

        public Operation(Command command, String key, String value) {
            this.command = command;
            this.key = key;
            this.value = value;
        }

    // Semantic  is operation and arguments
    //
        static Operation parse (String line) throws IllegalArgumentException{
        try{

            String[] values = line.split(" ", 3);
            Operation op = new Operation(Command.valueOf(values[0]), 
                    values.length > 1 ? values[1] : null, 
                    values.length>2?values[2]:null);
            op.validate();
            return op;

        }catch (IllegalArgumentException e){
            throw e;
        }catch (Exception e){
            throw new IllegalArgumentException("Invalid command");
        }


    }

    public void validate() throws IllegalArgumentException {
        switch (command) {
            case SET:
                if(key==null || key.trim().equals("")){
                    throw new IllegalArgumentException("Key cannot be empty");
                }
                if ( value == null) {
                    throw new IllegalArgumentException("SET requires key and value");
                }
                break;
            case GET:
                if(value != null){
                    throw new IllegalArgumentException("GET does not accept a value");
                }
            case DEL:
                if (key == null) {
                    throw new IllegalArgumentException(command + " requires a key");
                }
                break;
            case EXIT:
                // No arguments required
                break;
        }
    }

    public String getLine() {
        return command + " " + getKey() + " " + getValue();
    }

    public Command getCommand() {
        return command;
    }

    public String getKey() {
        return key!=null?key:"";
    }

    public String getValue() {
        return value!=null?value:"";
    }

    @Override
    public String toString() {
        return "Operation{" +
                "command=" + command +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
