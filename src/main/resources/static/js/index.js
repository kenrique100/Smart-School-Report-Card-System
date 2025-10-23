document.addEventListener("DOMContentLoaded", function () {

    // --- 1. Highlight failing grades on report tables ---
    const gradeCells = document.querySelectorAll("td");
    gradeCells.forEach(cell => {
        const text = cell.textContent.trim();
        // Check for failing grades (F) or low scores
        if (text === 'F' || text === 'FAIL') {
            cell.style.color = "red";
            cell.style.fontWeight = "bold";
        }

        // Check for numeric scores below 10
        const value = parseFloat(text);
        if (!isNaN(value) && value < 10) {
            cell.style.color = "red";
            cell.style.fontWeight = "bold";
        }
    });

    // --- 2. Confirm deletion links ---
    const deleteLinks = document.querySelectorAll("a.delete-link");
    deleteLinks.forEach(link => {
        link.addEventListener("click", function (e) {
            const name = this.getAttribute("data-name");
            if (!confirm("Are you sure you want to delete " + name + "?")) {
                e.preventDefault();
            }
        });
    });

    // --- 3. Dynamic calculation of term average in assessment form ---
    const scoreInputs = document.querySelectorAll("input[name^='score_']");
    const avgDisplay = document.getElementById("term-average");

    if (scoreInputs.length && avgDisplay) {
        function calculateAverage() {
            let total = 0;
            let count = 0;
            scoreInputs.forEach(input => {
                const val = parseFloat(input.value);
                if (!isNaN(val)) {
                    total += val;
                    count++;
                }
            });
            const avg = count > 0 ? (total / count).toFixed(2) : 0;
            avgDisplay.textContent = avg;
        }

        scoreInputs.forEach(input => {
            input.addEventListener("input", calculateAverage);
        });
    }

    // --- 4. Auto-calculate grade based on marks ---
    const marksInput = document.querySelector('input[name="marks"]');
    const gradeSelect = document.querySelector('select[name="grade"]');

    if (marksInput && gradeSelect) {
        marksInput.addEventListener('input', function() {
            const marks = parseFloat(this.value);
            if (!isNaN(marks)) {
                let grade = 'F';
                if (marks >= 90) grade = 'A+';
                else if (marks >= 80) grade = 'A';
                else if (marks >= 70) grade = 'B+';
                else if (marks >= 60) grade = 'B';
                else if (marks >= 50) grade = 'C+';
                else if (marks >= 40) grade = 'C';

                gradeSelect.value = grade;
            }
        });
    }

    // --- 5. Table search functionality ---
    const searchInputs = document.querySelectorAll('input[placeholder="Search:"]');
    searchInputs.forEach(input => {
        input.addEventListener('input', function() {
            const searchTerm = this.value.toLowerCase();
            const table = this.closest('.bg-white').querySelector('table');
            const rows = table.querySelectorAll('tbody tr');

            rows.forEach(row => {
                const text = row.textContent.toLowerCase();
                row.style.display = text.includes(searchTerm) ? '' : 'none';
            });
        });
    });

    // --- 6. Print functionality for reports ---
    const printButtons = document.querySelectorAll('button[onclick="window.print()"]');
    printButtons.forEach(button => {
        button.addEventListener('click', function() {
            window.print();
        });
    });
});