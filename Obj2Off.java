import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Parser OBJ vers OFF
 */
public class Obj2Off {
    private BufferedReader buffRead;
    private BufferedWriter buffWrite;
    private List<Vertex> vertices;
    private List<Face> faces;
    private List<Texture> textures;

    public Obj2Off(FileReader reader, FileWriter writer) {
        this.vertices = new ArrayList<Vertex>();
        this.faces = new ArrayList<Face>();
        this.textures = new ArrayList<Texture>();
        this.buffRead = new BufferedReader(reader);
        this.buffWrite = new BufferedWriter(writer);
    }

    private class Vertex {
        private double x;
        private double y;
        private double z;
        private double u;
        private double v;

        public Vertex(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = 0;
            this.v = 0;
        }
        public void setTexture(Texture t) {
            this.u = t.getU();
            this.v = t.getV();
        }
        public String toString() {
            return x + " " + y + " " + z + " 0.0 0.0 0.0 " + u + " " + v; 
        }
    }

    private class Face {
        private int v1;
        private int v2;
        private int v3;

        public Face(int v1, int v2, int v3) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
        }
        public String toString() {
            return "3 " + this.v1 + " " + this.v2 + " " + this.v3;
        }
    }

    private class Texture {
        private double u;
        private double v;

        public Texture(double u, double v) {
            this.u = u;
            this.v = v;
        }
        public double getU() {
            return this.u;
        }
        public double getV() {
            return this.v;
        }
        public String toString() {
            return this.u + " " + this.v;
        }
    }

    public static void main (String[] args) {
        if(args.length != 1) {
            System.out.println("Usage: java Obj2Off <OBJ file>");
            System.exit(1);
        }

        FileReader reader = null;
        FileWriter writer = null;
        
        try {
            // Ouverture du fichier a parser
            reader = new FileReader(args[0]);
            // Ouverture du fichier a ecrire
            File output = new File(args[0].substring(0,args[0].length()-4) + ".off");
            writer = new FileWriter(output);
        } catch (Exception e) {
            System.out.println("Probleme lors de l'ouverture des fichiers.");
            System.exit(1);
        }

        // Instanciation du parser
        Obj2Off parser = new Obj2Off(reader,writer);

        // Lancement du parsing
        parser.parse();

        // Ecriture dans le fichier
        parser.writeFile();

        // Fermeture
        parser.close();
    }

    private void close() {
        try {
            buffWrite.close();
            buffRead.close();
        } catch (Exception e) {
            System.out.println("Probleme lors de la fermeture des fichiers.");
        }
    }

    private void writeFile() {
        try {
            buffWrite.write("OFF");
            buffWrite.newLine();
            buffWrite.write(vertices.size() + " " + faces.size() + " 0");
            buffWrite.newLine();
            buffWrite.newLine();

            for (Vertex v : vertices) {
                buffWrite.write(v.toString());
                buffWrite.newLine();
            }
            buffWrite.newLine();
            
            for (Face f : faces) {
                buffWrite.write(f.toString());
                buffWrite.newLine();
            }
        } catch (Exception e) {
            System.out.println("Probleme lors de l'ecriture dans le fichier.");
        }
    }

    private void parse() {
        String line = null;

        try {
            while ((line = buffRead.readLine()) != null) {
                String[] words = line.split(" ");

                switch (words[0]) {
                    case "mtllib": // material file name
                        // TODO
                    case "v":  // SOMMET [v x y z]
                        // Recuperation des coordonnees du sommet
                        double x = new Double(words[1]);
                        double y = new Double(words[2]);
                        double z = new Double(words[3]);
                        // Ajout du sommet a la liste
                        vertices.add(new Vertex(x,y,z));
                        break;

                    case "vn": // NORMALE
                        // Les normales sont calculees par notre renderer
                        break;

                    case "vt": // TEXTURE [vt u v]
                        // Recuperation des coordonnees de texture
                        double u = new Double(words[1]);
                        double v = new Double(words[2]);
                        // Ajout de la texture a la liste
                        textures.add(new Texture(u,v));
                        break;

                    case "f":  // FACE [f v1/vt1/vn1 v2/vt2/vn2 ...]
                        // Separation des 3 sommets
                        String[] vert1 = words[1].split("/");
                        String[] vert2 = words[2].split("/");
                        String[] vert3 = words[3].split("/");

                        // Recuperation des numeros de sommets et de texture
                        int numV1 = new Integer(vert1[0]);
                        int numV2 = new Integer(vert2[0]);
                        int numV3 = new Integer(vert3[0]);
                        int numT1 = new Integer(vert1[1]);
                        int numT2 = new Integer(vert2[1]);
                        int numT3 = new Integer(vert3[1]);

                        // Ajout de la face a la liste
                        faces.add(new Face(numV1-1,numV2-1,numV3-1));
                        // Ajout des textures aux sommets
                        vertices.get(numV1-1).setTexture(textures.get(numT1-1));
                        vertices.get(numV2-1).setTexture(textures.get(numT2-1));
                        vertices.get(numV3-1).setTexture(textures.get(numT3-1));
                        break;

                    default:
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Probleme lors du parsing.");
            System.exit(1);
        }
    }
}
