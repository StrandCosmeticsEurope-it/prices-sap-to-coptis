import com.sap.conn.jco.JCoDestination
import com.sap.conn.jco.JCoDestinationManager
import com.sap.conn.jco.JCoFunction
import com.sap.conn.jco.JCoTable
import java.nio.file.Files
import java.nio.file.Paths

class SAP {
    private String destinationName
    private String tableName
    private String delimiter = "|"
    private List<String> fields = []
    private List<String> options = []

    static void call(String destinationName, Closure closure) {
        SAP sap = new SAP(destinationName)
        closure.delegate = sap
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
    }

    SAP(String destinationName) {
        this.destinationName = destinationName
    }

    def table(String name) {
        this.tableName = name
    }

    def delimiter(String delimiter) {
        this.delimiter = delimiter
    }

    def options(Closure closure) {
        closure.delegate = this
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
    }

    def option(String option) {
        this.options << option
    }

    def fields(String... fieldNames) {
        this.fields.addAll(fieldNames)
    }

    def execute(Closure closure) {
        JCoDestination destination = JCoDestinationManager.getDestination(destinationName)
        JCoFunction function = destination.getRepository().getFunction("RFC_READ_TABLE")

        if (!function) {
            throw new RuntimeException("RFC_READ_TABLE not found in SAP.")
        }

        function.getImportParameterList().with {
            setValue("QUERY_TABLE", tableName)
            setValue("DELIMITER", delimiter)
        }

        // Set OPTIONS table parameter
        function.getTableParameterList().getTable("OPTIONS").with {
            options.each { optionText ->
                appendRow()
                setValue("TEXT", optionText)
            }
        }

        // Set FIELDS table parameter
        function.getTableParameterList().getTable("FIELDS").with {
            fields.each { fieldName ->
                appendRow()
                setValue("FIELDNAME", fieldName)
            }
        }

        // Execute the function
        function.execute(destination)

        // Get the DATA table parameter which contains the results
        JCoTable dataTable = function.getTableParameterList().getTable("DATA")

        // Collect the data into a list of strings
        def resultLines = (0..<dataTable.getNumRows()).collect { i ->
            dataTable.setRow(i)
            def rowData = dataTable.getString("WA")
            def fields = rowData.split("\\|")
            if (fields.length == 1) {
              fields += "" // Add empty string for trailing delimiter
            }
            if (fields.length >= 2) {
                fields.collect { it.trim() }.join('\t')
            } else {
                println "Error with rowData: $rowData"
                throw new RuntimeException("Incorrect number of fields returned by RFC_READ_TABLE for ${tableName} table")
            }
        }

        // Execute additional closure logic (like saving to file)
        closure.delegate = new ResultHandler(fields, resultLines)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
    }
}

class ResultHandler {
    private List<String> fields
    private List<String> resultLines

    ResultHandler(List<String> fields, List<String> resultLines) {
        this.fields = fields
        this.resultLines = resultLines
    }

    def writeToFile(String outputFilePath) {
        def header = fields.join('\t')
        Files.write(Paths.get(outputFilePath), [header] + resultLines)
    }
}

// Read MARA
SAP.call("MVP") {
    table "MARA"
    delimiter "|"
    options {
        option "MATKL LIKE 'M3%'"
    }
    fields "MATNR", "BISMT", "MEINS"
    execute {
        writeToFile "data/mara.txt"
    }
}

// Read MARC
SAP.call("MVP") {
    table "MARC"
    delimiter "|"
    options {
        option "WERKS = 'CH21'"
        option "AND MMSTA = 'Z5'"
        option "AND BESKZ = 'F'"
    }
    fields "MATNR"
    execute {
        writeToFile "data/marc.txt"
    }
}

// Read MBEW
SAP.call("MVP") {
    table "MBEW"
    delimiter "|"
    options {
        option "VERPR > '0.0'"
        option "AND BWKEY = 'CH21'"
        option "AND BWTAR = 'INTERNAL'"
    }
    fields "MATNR", "VERPR", "PEINH"
    execute {
        writeToFile "data/mbew.txt"
    }
}

