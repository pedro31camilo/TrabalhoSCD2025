public class Message {
    public static final int TAMANHO_MENSAGEM = 10;
    public static final String SEPARADOR = "|";
    public static final int REQUEST = 1;
    public static final int GRANT = 2;
    public static final int RELEASE = 3;


    public final int type;
    public final int processId;

    public Message(int type, int processId){
        this.type = type;
        this.processId = processId;
    }

    public int getType() { return type; }
    public int getProcessId() { return processId; }

    public String transformaMessage(){ //Transforma o Objeto Message em String
        String message = type + SEPARADOR + processId + SEPARADOR;

        int zeros = TAMANHO_MENSAGEM - message.length();

        String filling = String.format("%" + zeros + "s", "").replace(" ", "0");
        return message + filling;
    }

    public static Message transformaString(String mensagem){
        String[] texts = mensagem.split("\\" + SEPARADOR);
        if(texts.length >= 2) {
            try{
                int type = Integer.parseInt(texts[0]);
                int processId = Integer.parseInt(texts[1]);
                return new Message(type, processId);
            } catch (NumberFormatException e){
                System.err.println("Parsing error: " + mensagem);
            }
        }
        return null;
    }

    @Override
    public String toString(){
        String typeStr;
        if(type == REQUEST){
            typeStr = "REQUEST";
        } else if(type == GRANT){
            typeStr = "GRANT";
        } else typeStr = "RELEASE";
        return String.format("%s (ID: %d)", typeStr, processId);
    }
}
