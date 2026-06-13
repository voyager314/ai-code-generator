const fs = require('fs');
let content = fs.readFileSync('D:/14297/idea_projects/wise-code/frontend/src/pages/AppChat.part1.tsx', 'utf-8');
const part2 = fs.readFileSync('D:/14297/idea_projects/wise-code/frontend/appchat_backup.tsx', 'utf-8');

const returnIdx = content.lastIndexOf('  return (');
if (returnIdx > -1) {
  content = content.substring(0, returnIdx);
}
fs.writeFileSync('D:/14297/idea_projects/wise-code/frontend/src/pages/AppChat.tsx', content + "\n}\n");
