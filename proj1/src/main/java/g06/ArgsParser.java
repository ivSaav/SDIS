package main.java.g06;

public class ArgsParser {

    private static Definitions.Operation oper;

    public static void validateArguments(String[] args) {
        if (args.length < 3) {
            System.out.println("usage: TestApp <peer_app> <operation> <opend_1> <opend_2>");
            System.exit(1);
        }

        try {
            oper = Definitions.Operation.valueOf(args[1]);
        }
        catch (IllegalArgumentException e) {
            System.out.println(e.toString());
            System.exit(1);
        }


        if (oper == Definitions.Operation.BACKUP) {
            if (args.length != 5) {
                System.out.println("usage: TestApp <peer_app> BACKUP <filename> <replication_deg>");
                System.exit(1);
            }
        }

        String message = args[0];
        int peer_app = Integer.parseInt(args[1]);

        String operation = args[1];
        String filename = "";
        int rep_degree = 0, disk_size = 0;
        if (operation.equals("BACKUP")) {
            if (args.length != 5) {
                System.out.println("usage: TestApp <peer_app> BACKUP <filename> <replication_deg>");
                System.exit(1);
            }

            filename = args[2];
            rep_degree = Integer.parseInt(args[3]);
        }
        else if (operation.equals("RESTORE")){
            if (args.length != 4) {
                System.out.println("usage: TestApp <peer_app> RESTORE <filename>");
                System.exit(1);
            }
            filename = args[2];
        }
        else if (operation.equals("DELETE")) {
            if (args.length != 4){
                System.out.println("usage: TestApp <peer_app> DELETE <filename>");
            }
            filename = args[2];
        }
        else if (operation.equals("RECLAIM")) {
            if (args.length != 2){
                System.out.println("usage: TestApp <peer_app> RECLAIM <filename>");
                System.exit(0);
                disk_size = Integer.parseInt(args[2]);
            }
        }
        else {
            System.out.println("Unknown operation");
            System.exit(0);
        }
    }
}